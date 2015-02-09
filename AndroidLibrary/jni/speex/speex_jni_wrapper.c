#include <jni.h>
#include <unistd.h>
#include <android/log.h>

#include <speex/speex.h>
#include <speex/speex_echo.h>
#include "speex/speex_preprocess.h"

/* encoder */
void *enc = NULL;
static SpeexBits encBits;
static int encFrameSize;

/* decoder */
void *dec = NULL;
static SpeexBits decBits;
static int decFrameSize;

/* AEC */
static SpeexEchoState *echoSt = NULL;
static SpeexPreprocessState *prep = NULL;
static int echoFrameSize;

#define TAG "SpeexJNI"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__);

JNIEXPORT jint JNICALL Java_com_thinktube_jni_Speex_encoderCreate
(JNIEnv *env, jobject obj, jint compression)
{
	if (enc != NULL)
		return (jint)-1;

	speex_bits_init(&encBits);

	enc = speex_encoder_init(&speex_nb_mode);

	speex_encoder_ctl(enc, SPEEX_SET_QUALITY, &compression);
	speex_encoder_ctl(enc, SPEEX_GET_FRAME_SIZE, &encFrameSize);

	LOGD("Speex encoder initialized with frame size %d", encFrameSize);
	return (jint)0;
}

JNIEXPORT jint JNICALL Java_com_thinktube_jni_Speex_encode
(JNIEnv *env, jobject obj,
jshortArray input, jint inOffset, jint inSize,
jbyteArray output, jint outOffset, jint outSize)
{
	jshort *inBuffer;
	jbyte *outBuffer;
	jboolean isCopy;
	int nsamples = (inSize-1)/encFrameSize + 1;
	int i, len = 0;

	if (enc == NULL)
		return -1;

	speex_bits_reset(&encBits);

	inBuffer = (*env)->GetShortArrayElements(env, input, &isCopy);
	outBuffer = (*env)->GetByteArrayElements(env, output, &isCopy);

	// this allows us to encode more than 1 frame at a time
	for (i = 0; i < nsamples; i++) {
		speex_encode_int(enc, &inBuffer[inOffset + i*encFrameSize], &encBits);
	}

	len = speex_bits_write(&encBits, &outBuffer[outOffset], outSize);

	(*env)->ReleaseShortArrayElements(env, input, inBuffer, JNI_ABORT);
	(*env)->ReleaseByteArrayElements(env, output, outBuffer, 0);

	return (jint)len;
}

JNIEXPORT void JNICALL Java_com_thinktube_jni_Speex_encoderDestroy
(JNIEnv *env, jobject obj)
{
	if (enc == NULL)
		return;

	speex_bits_destroy(&encBits);
	speex_encoder_destroy(enc);
	enc = NULL;
}

/* decoder */

JNIEXPORT jint JNICALL Java_com_thinktube_jni_Speex_decoderCreate
(JNIEnv *env, jobject obj)
{
	if (dec != NULL)
		return (jint)-1;

	speex_bits_init(&decBits);

	dec = speex_decoder_init(&speex_nb_mode);

	speex_decoder_ctl(dec, SPEEX_GET_FRAME_SIZE, &decFrameSize);

	LOGD("Speex decoder initialized with frame size %d", decFrameSize);
	return (jint)0;
}

JNIEXPORT jint JNICALL Java_com_thinktube_jni_Speex_decode
(JNIEnv *env, jobject obj,
jbyteArray input, jint inOffset, jint inSize,
jshortArray output, jint outOffset, jint outSize)
{
	jbyte *inBuffer;
	jshort *outBuffer;
	jboolean isCopy;
	int nsamples = (outSize-1)/decFrameSize + 1;
	int i, ret;

	if (dec == NULL)
		return -1;

	inBuffer = (*env)->GetByteArrayElements(env, input, &isCopy);
	outBuffer = (*env)->GetShortArrayElements(env, output, &isCopy);

	speex_bits_read_from(&decBits, &inBuffer[inOffset], inSize);

	// this allows us to decode more than 1 frame at a time
	for (i = 0; i < nsamples; i++) {
		// if &dbit is NULL speex interpolates the data (packet loss concealment)
		ret = speex_decode_int(dec, &decBits, &outBuffer[outOffset + i*decFrameSize]);
		if (ret < 0)
			break;
	}

	(*env)->ReleaseByteArrayElements(env, input, inBuffer, JNI_ABORT);
	(*env)->ReleaseShortArrayElements(env, output, outBuffer, 0);

	return (jint)(decFrameSize * i);
}

JNIEXPORT void JNICALL Java_com_thinktube_jni_Speex_decoderDestroy
(JNIEnv *env, jobject obj)
{
	if (dec == NULL)
		return;

	speex_bits_destroy(&decBits);
	speex_decoder_destroy(dec);
	dec = NULL;
}

/*
 * References for the code below:
 * https://github.com/yzf/Codes/blob/7a502ccf868d89cf0817453dfae421c79816b26b/VoIP_Client_Speex/jni/speex_jni.cpp
 * http://stackoverflow.com/questions/12748277/speex-echo-cancellation-configuration
 */

JNIEXPORT jint JNICALL Java_com_thinktube_jni_Speex_echoCreate
(JNIEnv *env, jobject obj,
jint sample_rate, jint frame_size, jint filter_length)
{
	if (echoSt != NULL || prep != NULL)
			return (jint)-1;

	echoFrameSize = frame_size;

	echoSt = speex_echo_state_init(frame_size, filter_length);
	speex_echo_ctl(echoSt, SPEEX_ECHO_SET_SAMPLING_RATE, &sample_rate);
	LOGD("Speex echo initialized with sample rate %d, frame size %d and filter length %d",
		 sample_rate, echoFrameSize, filter_length);

	prep = speex_preprocess_state_init(frame_size, sample_rate);
	speex_preprocess_ctl(prep, SPEEX_PREPROCESS_SET_ECHO_STATE, echoSt);

	int nsOn = 1; // TODO: make this a parameter
	if ( nsOn) {
		speex_preprocess_ctl(prep, SPEEX_PREPROCESS_SET_DENOISE, &nsOn);
		int nsLvl = -25; // [-X..0] default = -15
		speex_preprocess_ctl(prep, SPEEX_PREPROCESS_SET_ECHO_SUPPRESS_ACTIVE, &nsLvl);
		LOGD("Speex NS enabled with level %d", nsLvl);
	}

	int agcOn = 1; // TODO: make this a parameter
	if ( agcOn ) {
		speex_preprocess_ctl(prep, SPEEX_PREPROCESS_SET_AGC, &agcOn);
		int agcLvl = 22000; // [0..32768] default = 8000
		speex_preprocess_ctl(prep, SPEEX_PREPROCESS_SET_AGC_LEVEL, &agcLvl);
		LOGD("Speex AGC enabled with level %d", agcLvl);
	}

	return (jint) 0;
}

JNIEXPORT jint JNICALL Java_com_thinktube_jni_Speex_echoPlayback
(JNIEnv * env, jobject obj,
jshortArray input, jint inSize)
{
	jshort *inBuffer;
	jboolean isCopy;
	int i, nsamples = (inSize-1)/echoFrameSize + 1;

	if (echoSt == NULL || prep == NULL)
				return (jint)-1;

	inBuffer = (*env)->GetShortArrayElements(env, input, &isCopy);

	for (i = 0; i < nsamples; i++) {
		speex_echo_playback(echoSt, &inBuffer[i*echoFrameSize]);
	}

	(*env)->ReleaseShortArrayElements(env, input, inBuffer, JNI_ABORT);

	return nsamples * echoFrameSize;
}

JNIEXPORT jint JNICALL Java_com_thinktube_jni_Speex_echoCapture
(JNIEnv * env, jobject obj,
jshortArray input, jshortArray output, jint inOutSize)
{
	jshort *inBuffer;
	jshort *outBuffer;
	jboolean isCopy;
	int i, nsamples = (inOutSize-1)/echoFrameSize + 1;

	inBuffer = (*env)->GetShortArrayElements(env, input, &isCopy);
	outBuffer = (*env)->GetShortArrayElements(env, output, &isCopy);

	for (i = 0; i < nsamples; i++) {
		speex_echo_capture(echoSt, &inBuffer[i*echoFrameSize], &outBuffer[i*echoFrameSize]);
		speex_preprocess_run(prep, &outBuffer[i*echoFrameSize]);
	}

	(*env)->ReleaseShortArrayElements(env, input, inBuffer, JNI_ABORT);
	(*env)->ReleaseShortArrayElements(env, output, outBuffer, 0);

	return nsamples * echoFrameSize;
}

JNIEXPORT jint JNICALL Java_com_thinktube_jni_Speex_echoCancelation
(JNIEnv * env, jobject obj,
jshortArray input, jshortArray echo, jshortArray output, jint inOutSize)
{
	jshort *inBuffer;
	jshort *echoBuffer;
	jshort *outBuffer;
	jboolean isCopy;
	int i, nsamples = (inOutSize-1)/echoFrameSize + 1;

	inBuffer = (*env)->GetShortArrayElements(env, input, &isCopy);
	echoBuffer = (*env)->GetShortArrayElements(env, echo, &isCopy);
	outBuffer = (*env)->GetShortArrayElements(env, output, &isCopy);

	for (i = 0; i < nsamples; i++) {
		speex_echo_cancellation(echoSt, &inBuffer[i*echoFrameSize], &echoBuffer[i*echoFrameSize], &outBuffer[i*echoFrameSize]);
		speex_preprocess_run(prep, &outBuffer[i*echoFrameSize]);
	}

	(*env)->ReleaseShortArrayElements(env, input, inBuffer, JNI_ABORT);
	(*env)->ReleaseShortArrayElements(env, echo, echoBuffer, JNI_ABORT);
	(*env)->ReleaseShortArrayElements(env, output, outBuffer, 0);

	return nsamples * echoFrameSize;
}

JNIEXPORT void JNICALL Java_com_thinktube_jni_Speex_echoDestroy
(JNIEnv *env, jobject obj)
{
	if (echoSt != NULL && prep != NULL) {
		speex_echo_state_destroy(echoSt);
		speex_preprocess_state_destroy(prep);
	}
}
