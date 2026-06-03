#include <jni.h>
#include <openssl/evp.h>
#include <openssl/rand.h>
#include <openssl/aes.h>
#include <vector>
#include <cstring>
#include <android/log.h>

#define TAG "CryptoCore"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)

// ─── AES-256-CBC encrypt (raw bytes) ──────────────────────────────────────────
static std::vector<uint8_t> aes256_cbc_encrypt(
        const uint8_t* key, const uint8_t* iv,
        const uint8_t* pt,  size_t pt_len) {

    EVP_CIPHER_CTX* ctx = EVP_CIPHER_CTX_new();
    std::vector<uint8_t> out(pt_len + AES_BLOCK_SIZE);
    int out_len = 0, final_len = 0;

    EVP_EncryptInit_ex(ctx, EVP_aes_256_cbc(), nullptr, key, iv);
    EVP_CIPHER_CTX_set_padding(ctx, 1); // PKCS7 padding on
    EVP_EncryptUpdate(ctx, out.data(), &out_len, pt, (int)pt_len);
    EVP_EncryptFinal_ex(ctx, out.data() + out_len, &final_len);
    EVP_CIPHER_CTX_free(ctx);

    out.resize(out_len + final_len);
    return out;
}

// ─── AES-256-CBC decrypt ──────────────────────────────────────────────────────
static std::vector<uint8_t> aes256_cbc_decrypt(
        const uint8_t* key, const uint8_t* iv,
        const uint8_t* ct,  size_t ct_len) {

    EVP_CIPHER_CTX* ctx = EVP_CIPHER_CTX_new();
    std::vector<uint8_t> out(ct_len);
    int out_len = 0, final_len = 0;

    EVP_DecryptInit_ex(ctx, EVP_aes_256_cbc(), nullptr, key, iv);
    EVP_DecryptUpdate(ctx, out.data(), &out_len, ct, (int)ct_len);
    EVP_DecryptFinal_ex(ctx, out.data() + out_len, &final_len);
    EVP_CIPHER_CTX_free(ctx);

    out.resize(out_len + final_len);
    return out;
}

// ─── JNI: encryptBytes ────────────────────────────────────────────────────────
extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_sysupdate_CryptoCore_encryptBytes(JNIEnv* env, jclass,
                                            jbyteArray jKey,
                                            jbyteArray jIv,
                                            jbyteArray jData) {
    jsize kLen = env->GetArrayLength(jKey);
    jsize iLen = env->GetArrayLength(jIv);
    jsize dLen = env->GetArrayLength(jData);

    std::vector<uint8_t> key(kLen), iv(iLen), data(dLen);
    env->GetByteArrayRegion(jKey,  0, kLen, (jbyte*)key.data());
    env->GetByteArrayRegion(jIv,   0, iLen, (jbyte*)iv.data());
    env->GetByteArrayRegion(jData, 0, dLen, (jbyte*)data.data());

    auto enc = aes256_cbc_encrypt(key.data(), iv.data(),
                                   data.data(), data.size());

    jbyteArray ret = env->NewByteArray((jsize)enc.size());
    env->SetByteArrayRegion(ret, 0, (jsize)enc.size(), (jbyte*)enc.data());
    return ret;
}

// ─── JNI: decryptBytes ────────────────────────────────────────────────────────
extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_sysupdate_CryptoCore_decryptBytes(JNIEnv* env, jclass,
                                            jbyteArray jKey,
                                            jbyteArray jIv,
                                            jbyteArray jData) {
    jsize kLen = env->GetArrayLength(jKey);
    jsize iLen = env->GetArrayLength(jIv);
    jsize dLen = env->GetArrayLength(jData);

    std::vector<uint8_t> key(kLen), iv(iLen), data(dLen);
    env->GetByteArrayRegion(jKey,  0, kLen, (jbyte*)key.data());
    env->GetByteArrayRegion(jIv,   0, iLen, (jbyte*)iv.data());
    env->GetByteArrayRegion(jData, 0, dLen, (jbyte*)data.data());

    auto dec = aes256_cbc_decrypt(key.data(), iv.data(),
                                   data.data(), data.size());

    jbyteArray ret = env->NewByteArray((jsize)dec.size());
    env->SetByteArrayRegion(ret, 0, (jsize)dec.size(), (jbyte*)dec.data());
    return ret;
}

// ─── JNI: generateSecureRandom ───────────────────────────────────────────────
extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_sysupdate_CryptoCore_generateSecureRandom(JNIEnv* env, jclass,
                                                    jint length) {
    std::vector<uint8_t> buf(length);
    RAND_bytes(buf.data(), length);
    jbyteArray ret = env->NewByteArray(length);
    env->SetByteArrayRegion(ret, 0, length, (jbyte*)buf.data());
    return ret;
}
