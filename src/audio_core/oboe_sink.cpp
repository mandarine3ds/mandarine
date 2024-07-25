#include "oboe_sink.h"

#include <memory>
#include <oboe/Oboe.h>

#include "audio_core/audio_types.h"
#include "common/logging/log.h"

namespace AudioCore {

class OboeSink::Impl : public oboe::AudioStreamCallback {
public:
    Impl() = default;
    ~Impl() override {
        // Destructor now ensures that the stream is properly stopped and closed
        if (mStream && mStream->getState() != oboe::StreamState::Closed) {
            mStream->stop();
            mStream->close();
            mStream.reset();
        }
    }

    oboe::DataCallbackResult onAudioReady(oboe::AudioStream* oboeStream, void* audioData,
                                          int32_t numFrames) override {
        s16* outputBuffer = static_cast<s16*>(audioData);
        if (mCallback) {
            mCallback(outputBuffer, static_cast<std::size_t>(numFrames));
        }
        return oboe::DataCallbackResult::Continue;
    }

    void onErrorAfterClose(oboe::AudioStream* /* oboeStream */, oboe::Result error) override {
        if (error == oboe::Result::ErrorDisconnected) {
            LOG_INFO(Audio_Sink, "Restarting AudioStream after disconnect");
            start();
        } else {
            LOG_CRITICAL(Audio_Sink, "Error after close: {}", error);
        }
    }

    bool start() {
        if (mStream && mStream->getState() != oboe::StreamState::Closed) {
            mStream->stop();
            mStream->close();
        }
        if (mStream) {
            mStream.reset();
        }
        oboe::AudioStreamBuilder builder;
        auto result = builder.setSharingMode(oboe::SharingMode::Exclusive)
                          ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
                          ->setUsage(oboe::Usage::Game)
                          ->setFormat(oboe::AudioFormat::I16)
                          ->setSampleRate(mSampleRate)
                          ->setChannelCount(2)
                          ->setCallback(this)
                          ->openStream(mStream);
        if (result != oboe::Result::OK) {
            LOG_CRITICAL(Audio_Sink, "Error creating playback stream: {}",
                         oboe::convertToText(result));
            return false;
        }
        mSampleRate = mStream->getSampleRate();
        result = mStream->start();
        if (result != oboe::Result::OK) {
            LOG_CRITICAL(Audio_Sink, "Error starting playback stream: {}",
                         oboe::convertToText(result));
            return false;
        }
        return true;
    }

    void stop() {
        if (mStream && mStream->getState() != oboe::StreamState::Closed) {
            auto stopResult = mStream->stop();
            auto closeResult = mStream->close();
            if (stopResult != oboe::Result::OK) {
                LOG_CRITICAL(Audio_Sink, "Error stopping playback stream: {}",
                             oboe::convertToText(stopResult));
            }
            if (closeResult != oboe::Result::OK) {
                LOG_CRITICAL(Audio_Sink, "Error closing playback stream: {}",
                             oboe::convertToText(closeResult));
            }
            mStream.reset();
        }
    }

    int32_t GetSampleRate() const {
        return mSampleRate;
    }

    void SetCallback(std::function<void(s16*, std::size_t)> cb) {
        mCallback = cb;
    }

private:
    std::shared_ptr<oboe::AudioStream> mStream;
    std::function<void(s16*, std::size_t)> mCallback;
    int32_t mSampleRate = native_sample_rate;
};

OboeSink::OboeSink(std::string_view device_id) : impl(std::make_unique<Impl>()) {}
OboeSink::~OboeSink() {
    // Ensures resources are freed up when OboeSink is destroyed
    if (impl) {
        impl->stop();
    }
}

unsigned int OboeSink::GetNativeSampleRate() const {
    return impl->GetSampleRate();
}

void OboeSink::SetCallback(std::function<void(s16*, std::size_t)> cb) {
    impl->SetCallback(cb);
    impl->start();
}

std::vector<std::string> ListOboeSinkDevices() {
    return {"auto"};
}

} // namespace AudioCore
