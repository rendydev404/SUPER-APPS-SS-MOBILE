#include <android/asset_manager_jni.h>
#include <android/log.h>
#include <jni.h>
#include <cpu.h>
#include <net.h>

#include <cmath>
#include <memory>
#include <mutex>

namespace {
constexpr char kTag[] = "ArcFaceNCNN";
constexpr int kInputSize = 112;
constexpr int kEmbeddingSize = 512;

struct ArcFaceEngine {
    ncnn::Net net;
    std::mutex mutex;
};

void logError(const char* message) { __android_log_print(ANDROID_LOG_ERROR, kTag, "%s", message); }
}  // namespace

extern "C" JNIEXPORT jlong JNICALL
Java_com_sukashawarma_superapp_domain_face_NcnnArcFaceEmbeddingExtractor_nativeCreate(
        JNIEnv* env, jobject, jobject asset_manager) {
    AAssetManager* manager = AAssetManager_fromJava(env, asset_manager);
    if (manager == nullptr) return 0;
    auto engine = std::make_unique<ArcFaceEngine>();
    engine->net.opt.num_threads = ncnn::get_big_cpu_count();
    engine->net.opt.use_packing_layout = true;
    if (engine->net.load_param(manager, "arcface/w600k_mbf.ncnn.param") != 0 ||
        engine->net.load_model(manager, "arcface/w600k_mbf.ncnn.bin") != 0) {
        logError("Unable to load bundled ArcFace NCNN model");
        return 0;
    }
    return reinterpret_cast<jlong>(engine.release());
}

extern "C" JNIEXPORT void JNICALL
Java_com_sukashawarma_superapp_domain_face_NcnnArcFaceEmbeddingExtractor_nativeDestroy(
        JNIEnv*, jobject, jlong handle) {
    delete reinterpret_cast<ArcFaceEngine*>(handle);
}

extern "C" JNIEXPORT jfloatArray JNICALL
Java_com_sukashawarma_superapp_domain_face_NcnnArcFaceEmbeddingExtractor_nativeExtract(
        JNIEnv* env, jobject, jlong handle, jobject bitmap) {
    auto* engine = reinterpret_cast<ArcFaceEngine*>(handle);
    if (engine == nullptr || bitmap == nullptr) return nullptr;
    // NCNN handles Android Bitmap's native channel layout and stride itself.
    ncnn::Mat input = ncnn::Mat::from_android_bitmap(env, bitmap, ncnn::Mat::PIXEL_RGB);
    if (input.empty()) return nullptr;
    const float mean[3] = {127.5f, 127.5f, 127.5f};
    const float norm[3] = {1.f / 128.f, 1.f / 128.f, 1.f / 128.f};
    input.substract_mean_normalize(mean, norm);

    ncnn::Mat output;
    {
        std::lock_guard<std::mutex> lock(engine->mutex);
        ncnn::Extractor extractor = engine->net.create_extractor();
        if (extractor.input("in0", input) != 0 || extractor.extract("out0", output) != 0) return nullptr;
    }
    if (output.total() != kEmbeddingSize) return nullptr;

    const float* values = output;
    float squared_sum = 0.f;
    for (int i = 0; i < kEmbeddingSize; ++i) squared_sum += values[i] * values[i];
    const float length = std::sqrt(squared_sum);
    if (length <= 0.f) return nullptr;

    float normalized[kEmbeddingSize];
    for (int i = 0; i < kEmbeddingSize; ++i) normalized[i] = values[i] / length;
    jfloatArray result = env->NewFloatArray(kEmbeddingSize);
    if (result != nullptr) env->SetFloatArrayRegion(result, 0, kEmbeddingSize, normalized);
    return result;
}
