// Copyright 2024 Mandarine Project
// Licensed under GPLv2 or any later version
// Refer to the license.txt file included.

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
        if (m_stream && m_stream->getState() != oboe::StreamState::Closed) {
            m_stream->stop();
            m_stream->close();
            m_stream.reset();
        }
    }

    oboe::DataCallbackResult onAudioReady(oboe::AudioStream* /* oboeStream */, void* audio_data,
                                          int32_t num_frames) override {
        s16* output_buffer = static_cast<s16*>(audio_data);
        if (m_callback) {
            m_callback(output_buffer, static_cast<std::size_t>(num_frames));
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
        if (m_stream && m_stream->getState() != oboe::StreamState::Closed) {
            m_stream->stop();
            m_stream->close();
        }
        if (m_stream) {
            m_stream.reset();
        }
        oboe::AudioStreamBuilder builder;
        auto result = builder.setSharingMode(oboe::SharingMode::Exclusive)
                          ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
                          ->setAudioApi(oboe::AudioApi::OpenSLES)
                          ->setUsage(oboe::Usage::Game)
                          ->setFormat(oboe::AudioFormat::I16)
                          ->setFormatConversionAllowed(true)
                          ->setSampleRate(m_sample_rate)
                          ->setSampleRateConversionQuality(oboe::SampleRateConversionQuality::High)
                          ->setChannelCount(2)
                          ->setCallback(this)
                          ->openStream(m_stream);
        if (result != oboe::Result::OK) {
            LOG_CRITICAL(Audio_Sink, "Error creating playback stream: {}",
                         oboe::convertToText(result));
            return false;
        }
        m_sample_rate = m_stream->getSampleRate();
        result = m_stream->start();
        if (result != oboe::Result::OK) {
            LOG_CRITICAL(Audio_Sink, "Error starting playback stream: {}",
                         oboe::convertToText(result));
            return false;
        }
        return true;
    }

    void stop() {
        if (m_stream && m_stream->getState() != oboe::StreamState::Closed) {
            auto stop_result = m_stream->stop();
            auto close_result = m_stream->close();
            if (stop_result != oboe::Result::OK) {
                LOG_CRITICAL(Audio_Sink, "Error stopping playback stream: {}",
                             oboe::convertToText(stop_result));
            }
            if (close_result != oboe::Result::OK) {
                LOG_CRITICAL(Audio_Sink, "Error closing playback stream: {}",
                             oboe::convertToText(close_result));
            }
            m_stream.reset();
        }
    }

    int32_t GetSampleRate() const {
        return m_sample_rate;
    }

    void SetCallback(std::function<void(s16*, std::size_t)> cb) {
        m_callback = cb;
    }

private:
    std::shared_ptr<oboe::AudioStream> m_stream;
    std::function<void(s16*, std::size_t)> m_callback;
    int32_t m_sample_rate = native_sample_rate;
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
