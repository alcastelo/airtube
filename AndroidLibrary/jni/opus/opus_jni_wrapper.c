#include <jni.h>
#include <unistd.h>
#include <android/log.h>

#include "opus.h"

OpusEncoder *enc = NULL;
OpusDecoder *dec = NULL;

#define TAG "OpusJNI"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__);

JNIEXPORT jint JNICALL Java_com_thinktube_jni_OpusEncoderNative_create
(JNIEnv *env, jobject obj,
jint sampleRate, jint channels, jint app)
{
	int error;

	enc = opus_encoder_create(sampleRate, channels, app, &error);

	if (error == OPUS_OK) {
		LOGD("Opus encoder initialized");
	} else {
		LOGD("ERROR: Opus encoder could not be initialized!");
	}

	return (jint)error;
}

JNIEXPORT jint JNICALL Java_com_thinktube_jni_OpusEncoderNative_encode
(JNIEnv *env, jobject obj,
 jshortArray input, jint inOffset, jint inSize,
 jbyteArray output, jint outOffset, jint outSize)
{
	jshort* inBuffer;
	jbyte* outBuffer;
	jboolean isCopy;
	int len = 0;

	if (enc == NULL)
		return -1;

	inBuffer = (*env)->GetShortArrayElements(env, input, &isCopy);
	outBuffer = (*env)->GetByteArrayElements(env, output, &isCopy);

	len = opus_encode(enc, &inBuffer[inOffset], inSize, &outBuffer[outOffset], outSize);
	if (len < 0)
		LOGD("Encode error: %s", opus_strerror(len));

	(*env)->ReleaseShortArrayElements(env, input, inBuffer, JNI_ABORT);
	(*env)->ReleaseByteArrayElements(env, output, outBuffer, 0);

	return (jint)len;
}

JNIEXPORT void JNICALL Java_com_thinktube_jni_OpusEncoderNative_destroy
(JNIEnv *env, jobject obj)
{
	if (enc != NULL) {
		opus_encoder_destroy(enc);
		enc = NULL;
	}
}

JNIEXPORT jint JNICALL Java_com_thinktube_jni_OpusEncoderNative_getComplexity
(JNIEnv *env, jobject obj)
{
	int res;
	opus_encoder_ctl(enc, OPUS_GET_COMPLEXITY(&res));
	return (jint)res;
}

JNIEXPORT jint JNICALL Java_com_thinktube_jni_OpusEncoderNative_setComplexity
(JNIEnv *env, jobject obj, jint val)
{
	int res;
	res = opus_encoder_ctl(enc, OPUS_SET_COMPLEXITY(val));
	return (jint)res;
}

JNIEXPORT jint JNICALL Java_com_thinktube_jni_OpusEncoderNative_getBitrate
(JNIEnv *env, jobject obj)
{
	int res;
	opus_encoder_ctl(enc, OPUS_GET_BITRATE(&res));
	return (jint)res;
}

JNIEXPORT jint JNICALL Java_com_thinktube_jni_OpusEncoderNative_setBitrate
(JNIEnv *env, jobject obj, jint val)
{
	int res;
	res = opus_encoder_ctl(enc, OPUS_SET_BITRATE(val));
	return (jint)res;
}

JNIEXPORT jint JNICALL Java_com_thinktube_jni_OpusEncoderNative_getVBR
(JNIEnv *env, jobject obj)
{
	int res;
	opus_encoder_ctl(enc, OPUS_GET_VBR(&res));
	return (jint)res;
}

JNIEXPORT jint JNICALL Java_com_thinktube_jni_OpusEncoderNative_setVBR
(JNIEnv *env, jobject obj, jint val)
{
	int res;
	res = opus_encoder_ctl(enc, OPUS_SET_VBR(val));
	return (jint)res;
}

JNIEXPORT jint JNICALL Java_com_thinktube_jni_OpusEncoderNative_getVBRConstraint
(JNIEnv *env, jobject obj)
{
	int res;
	opus_encoder_ctl(enc, OPUS_GET_VBR_CONSTRAINT(&res));
	return (jint)res;
}

JNIEXPORT jint JNICALL Java_com_thinktube_jni_OpusEncoderNative_setVBRConstraint
(JNIEnv *env, jobject obj, jint val)
{
	int res;
	res = opus_encoder_ctl(enc, OPUS_SET_VBR_CONSTRAINT(val));
	return (jint)res;
}

JNIEXPORT jint JNICALL Java_com_thinktube_jni_OpusEncoderNative_getForceChannels
(JNIEnv *env, jobject obj)
{
	int res;
	opus_encoder_ctl(enc, OPUS_GET_FORCE_CHANNELS(&res));
	return (jint)res;
}

JNIEXPORT jint JNICALL Java_com_thinktube_jni_OpusEncoderNative_setForceChannels
(JNIEnv *env, jobject obj, jint val)
{
	int res;
	res = opus_encoder_ctl(enc, OPUS_SET_FORCE_CHANNELS(val));
	return (jint)res;
}

JNIEXPORT jint JNICALL Java_com_thinktube_jni_OpusEncoderNative_getMaxBandwidth
(JNIEnv *env, jobject obj)
{
	int res;
	opus_encoder_ctl(enc, OPUS_GET_MAX_BANDWIDTH(&res));
	return (jint)res;
}

JNIEXPORT jint JNICALL Java_com_thinktube_jni_OpusEncoderNative_setMaxBandwidth
(JNIEnv *env, jobject obj, jint val)
{
	int res;
	res = opus_encoder_ctl(enc, OPUS_SET_MAX_BANDWIDTH(val));
	return (jint)res;
}

JNIEXPORT jint JNICALL Java_com_thinktube_jni_OpusEncoderNative_getBandwidth
(JNIEnv *env, jobject obj)
{
	int res;
	opus_encoder_ctl(enc, OPUS_GET_BANDWIDTH(&res));
	return (jint)res;
}

JNIEXPORT jint JNICALL Java_com_thinktube_jni_OpusEncoderNative_setBandwidth
(JNIEnv *env, jobject obj, jint val)
{
	int res;
	res = opus_encoder_ctl(enc, OPUS_SET_BANDWIDTH(val));
	return (jint)res;
}

JNIEXPORT jint JNICALL Java_com_thinktube_jni_OpusEncoderNative_getSignal
(JNIEnv *env, jobject obj)
{
	int res;
	opus_encoder_ctl(enc, OPUS_GET_SIGNAL(&res));
	return (jint)res;
}

JNIEXPORT jint JNICALL Java_com_thinktube_jni_OpusEncoderNative_setSignal
(JNIEnv *env, jobject obj, jint val)
{
	int res;
	res = opus_encoder_ctl(enc, OPUS_SET_SIGNAL(val));
	return (jint)res;
}

JNIEXPORT jint JNICALL Java_com_thinktube_jni_OpusEncoderNative_getApplication
(JNIEnv *env, jobject obj)
{
	int res;
	opus_encoder_ctl(enc, OPUS_GET_APPLICATION(&res));
	return (jint)res;
}

JNIEXPORT jint JNICALL Java_com_thinktube_jni_OpusEncoderNative_setApplication
(JNIEnv *env, jobject obj, jint val)
{
	int res;
	res = opus_encoder_ctl(enc, OPUS_SET_APPLICATION(val));
	return (jint)res;
}

JNIEXPORT jint JNICALL Java_com_thinktube_jni_OpusEncoderNative_getSampleRate
(JNIEnv *env, jobject obj)
{
	int res;
	opus_encoder_ctl(enc, OPUS_GET_SAMPLE_RATE(&res));
	return (jint)res;
}

JNIEXPORT jint JNICALL Java_com_thinktube_jni_OpusEncoderNative_getLookahead
(JNIEnv *env, jobject obj)
{
	int res;
	opus_encoder_ctl(enc, OPUS_GET_LOOKAHEAD(&res));
	return (jint)res;
}

JNIEXPORT jint JNICALL Java_com_thinktube_jni_OpusEncoderNative_getInbandFEC
(JNIEnv *env, jobject obj)
{
	int res;
	opus_encoder_ctl(enc, OPUS_GET_INBAND_FEC(&res));
	return (jint)res;
}

JNIEXPORT jint JNICALL Java_com_thinktube_jni_OpusEncoderNative_setInbandFEC
(JNIEnv *env, jobject obj, jint val)
{
	int res;
	res = opus_encoder_ctl(enc, OPUS_SET_INBAND_FEC(val));
	return (jint)res;
}

JNIEXPORT jint JNICALL Java_com_thinktube_jni_OpusEncoderNative_getPacketLossPercentage
(JNIEnv *env, jobject obj)
{
	int res;
	opus_encoder_ctl(enc, OPUS_GET_PACKET_LOSS_PERC(&res));
	return (jint)res;
}

JNIEXPORT jint JNICALL Java_com_thinktube_jni_OpusEncoderNative_setPacketLossPercentage
(JNIEnv *env, jobject obj, jint val)
{
	int res;
	res = opus_encoder_ctl(enc, OPUS_SET_PACKET_LOSS_PERC(val));
	return (jint)res;
}

JNIEXPORT jint JNICALL Java_com_thinktube_jni_OpusEncoderNative_getDTX
(JNIEnv *env, jobject obj)
{
	int res;
	opus_encoder_ctl(enc, OPUS_GET_DTX(&res));
	return (jint)res;
}

JNIEXPORT jint JNICALL Java_com_thinktube_jni_OpusEncoderNative_setDTX
(JNIEnv *env, jobject obj, jint val)
{
	int res;
	res = opus_encoder_ctl(enc, OPUS_SET_DTX(val));
	return (jint)res;
}

JNIEXPORT jint JNICALL Java_com_thinktube_jni_OpusEncoderNative_getLsbDepth
(JNIEnv *env, jobject obj)
{
	int res;
	opus_encoder_ctl(enc, OPUS_GET_LSB_DEPTH(&res));
	return (jint)res;
}

JNIEXPORT jint JNICALL Java_com_thinktube_jni_OpusEncoderNative_setLsbDepth
(JNIEnv *env, jobject obj, jint val)
{
	int res;
	res = opus_encoder_ctl(enc, OPUS_SET_LSB_DEPTH(val));
	return (jint)res;
}

JNIEXPORT jint JNICALL Java_com_thinktube_jni_OpusEncoderNative_getExpertFrameDuration
(JNIEnv *env, jobject obj)
{
	int res;
	opus_encoder_ctl(enc, OPUS_GET_EXPERT_FRAME_DURATION(&res));
	return (jint)res;
}

JNIEXPORT jint JNICALL Java_com_thinktube_jni_OpusEncoderNative_setExpertFrameDuration
(JNIEnv *env, jobject obj, jint val)
{
	int res;
	res = opus_encoder_ctl(enc, OPUS_SET_EXPERT_FRAME_DURATION(val));
	return (jint)res;
}

JNIEXPORT jint JNICALL Java_com_thinktube_jni_OpusEncoderNative_getPredictionDisabled
(JNIEnv *env, jobject obj)
{
	int res;
	opus_encoder_ctl(enc, OPUS_GET_PREDICTION_DISABLED(&res));
	return (jint)res;
}

JNIEXPORT jint JNICALL Java_com_thinktube_jni_OpusEncoderNative_setPredictionDisabled
(JNIEnv *env, jobject obj, jint val)
{
	int res;
	res = opus_encoder_ctl(enc, OPUS_SET_PREDICTION_DISABLED(val));
	return (jint)res;
}

JNIEXPORT jint JNICALL Java_com_thinktube_jni_OpusEncoderNative_resetState
(JNIEnv *env, jobject obj, jint val)
{
	int res;
	res = opus_encoder_ctl(enc, OPUS_RESET_STATE);
	return (jint)res;
}

/* decoder */

JNIEXPORT jint JNICALL Java_com_thinktube_jni_OpusDecoderNative_create
(JNIEnv *env, jobject obj, jint sampleRate, jint channels)
{
	int error;
	dec = opus_decoder_create(sampleRate, channels, &error);

	LOGD("Opus decoder initialized");
	return (jint)0;
}

JNIEXPORT jint JNICALL Java_com_thinktube_jni_OpusDecoderNative_decode
(JNIEnv *env, jobject obj,
jbyteArray input, jint inOffset, jint inSize,
jshortArray output, jint outOffset, jint outSize, jint fec)
{
	jbyte *inBuffer;
	jshort *outBuffer;
	jboolean isCopy;
	int len;

	if (dec == NULL)
		return -1;

	inBuffer = (*env)->GetByteArrayElements(env, input, &isCopy);
	outBuffer = (*env)->GetShortArrayElements(env, output, &isCopy);

	if (inSize == 0)
		inBuffer = NULL;

	// if inBuffer=NULL and inSize=0 opus interpolates the data (packet loss concealment)
	len = opus_decode(dec, &inBuffer[inOffset], inSize, &outBuffer[outOffset], outSize, fec);
	if (len < 0)
		LOGD("Decode error: %s", opus_strerror(len));

	(*env)->ReleaseByteArrayElements(env, input, inBuffer, JNI_ABORT);
	(*env)->ReleaseShortArrayElements(env, output, outBuffer, 0);

	return (jint)len;
}

JNIEXPORT void JNICALL Java_com_thinktube_jni_OpusDecoderNative_destroy
(JNIEnv *env, jobject obj)
{
	if (dec != NULL) {
		opus_decoder_destroy(dec);
		dec = NULL;
	}
}

JNIEXPORT jint JNICALL Java_com_thinktube_jni_OpusDecoderNative_getBandwidth
(JNIEnv *env, jobject obj)
{
	int res;
	opus_decoder_ctl(dec, OPUS_GET_BANDWIDTH(&res));
	return (jint)res;
}

JNIEXPORT jint JNICALL Java_com_thinktube_jni_OpusDecoderNative_getSampleRate
(JNIEnv *env, jobject obj)
{
	int res;
	opus_decoder_ctl(dec, OPUS_GET_SAMPLE_RATE(&res));
	return (jint)res;
}

JNIEXPORT jint JNICALL Java_com_thinktube_jni_OpusDecoderNative_getPitch
(JNIEnv *env, jobject obj)
{
	int res;
	opus_decoder_ctl(dec, OPUS_GET_PITCH(&res));
	return (jint)res;
}

JNIEXPORT jint JNICALL Java_com_thinktube_jni_OpusDecoderNative_getGain
(JNIEnv *env, jobject obj)
{
	int res;
	opus_decoder_ctl(dec, OPUS_GET_GAIN(&res));
	return (jint)res;
}

JNIEXPORT jint JNICALL Java_com_thinktube_jni_OpusDecoderNative_setGain
(JNIEnv *env, jobject obj, jint val)
{
	int res;
	res = opus_decoder_ctl(dec, OPUS_SET_GAIN(val));
	return (jint)res;
}

JNIEXPORT jint JNICALL Java_com_thinktube_jni_OpusDecoderNative_getLastPacketDuration
(JNIEnv *env, jobject obj)
{
	int res;
	opus_decoder_ctl(dec, OPUS_GET_LAST_PACKET_DURATION(&res));
	return (jint)res;
}

JNIEXPORT jint JNICALL Java_com_thinktube_jni_OpusDecoderNative_resetState
(JNIEnv *env, jobject obj, jint val)
{
	int res;
	res = opus_decoder_ctl(dec, OPUS_RESET_STATE);
	return (jint)res;
}
