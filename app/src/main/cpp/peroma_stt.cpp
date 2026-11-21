// JNI bridge between Peroma Voice and transcribe.cpp, the engine behind Handy.
//
// Deliberately thin: it loads a model, runs one utterance of 16 kHz mono PCM
// and hands back the text. Recording, endpointing and everything else lives in
// Kotlin, where it can be tested without a device.

#include <jni.h>

#include <string>

#include "transcribe.h"

namespace {

std::string to_std(JNIEnv * env, jstring s) {
    if (s == nullptr) return {};
    const char * chars = env->GetStringUTFChars(s, nullptr);
    if (chars == nullptr) return {};
    std::string out(chars);
    env->ReleaseStringUTFChars(s, chars);
    return out;
}

void throw_illegal_state(JNIEnv * env, const std::string & message) {
    jclass cls = env->FindClass("java/lang/IllegalStateException");
    if (cls != nullptr) env->ThrowNew(cls, message.c_str());
}

}  // namespace

extern "C" {

// Returns an opaque handle, or throws IllegalStateException.
JNIEXPORT jlong JNICALL
Java_de_peroma_voice_LocalStt_nativeOpen(JNIEnv * env, jobject, jstring path, jint threads) {
    // Compute backends are compiled in for a static build, so this is a no-op;
    // it stays so a future dynamic build keeps working.
    transcribe_init_backends_default();

    transcribe_session_params sp;
    transcribe_session_params_init(&sp);
    sp.n_threads = threads;

    transcribe_session * session = nullptr;
    const std::string file = to_std(env, path);
    const transcribe_status st = transcribe_open(file.c_str(), nullptr, &sp, &session);
    if (st != TRANSCRIBE_OK || session == nullptr) {
        throw_illegal_state(env, std::string("transcribe_open failed: ") + transcribe_status_string(st));
        return 0;
    }
    return reinterpret_cast<jlong>(session);
}

// Transcribes one utterance. `language` is a short code such as "de", or empty
// to let the model detect it. Returns the text; throws on failure.
JNIEXPORT jstring JNICALL
Java_de_peroma_voice_LocalStt_nativeTranscribe(JNIEnv * env, jobject, jlong handle,
                                               jfloatArray pcm, jstring language) {
    auto * session = reinterpret_cast<transcribe_session *>(handle);
    if (session == nullptr || pcm == nullptr) {
        throw_illegal_state(env, "no model loaded");
        return nullptr;
    }

    const jsize n = env->GetArrayLength(pcm);
    if (n <= 0) return env->NewStringUTF("");

    jfloat * samples = env->GetFloatArrayElements(pcm, nullptr);
    if (samples == nullptr) return nullptr;  // OutOfMemoryError already pending

    const std::string lang = to_std(env, language);
    transcribe_run_params rp;
    transcribe_run_params_init(&rp);
    rp.task = TRANSCRIBE_TASK_TRANSCRIBE;
    rp.language = lang.empty() ? nullptr : lang.c_str();

    const transcribe_status st = transcribe_run(session, samples, n, &rp);
    env->ReleaseFloatArrayElements(pcm, samples, JNI_ABORT);

    // A truncated or looping decode still carries a usable partial transcript.
    if (st != TRANSCRIBE_OK && st != TRANSCRIBE_ERR_OUTPUT_TRUNCATED &&
        st != TRANSCRIBE_ERR_OUTPUT_REPETITION) {
        throw_illegal_state(env, std::string("transcribe_run failed: ") + transcribe_status_string(st));
        return nullptr;
    }

    const char * text = transcribe_full_text(session);
    return env->NewStringUTF(text != nullptr ? text : "");
}

JNIEXPORT void JNICALL
Java_de_peroma_voice_LocalStt_nativeClose(JNIEnv *, jobject, jlong handle) {
    transcribe_session_free(reinterpret_cast<transcribe_session *>(handle));
}

}  // extern "C"
