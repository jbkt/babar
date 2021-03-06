package com.bhnte.babar.agent.profiler;

import com.bhnte.babar.agent.config.AgentConfig;
import com.bhnte.babar.agent.reporter.Reporter;
import com.bhnte.babar.agent.worker.SamplingSchedulable;

import java.util.concurrent.atomic.AtomicLong;

public abstract class SamplingAggregatingProfiler extends SamplingProfiler implements SamplingSchedulable {

    private final long reportingIntervalMs;
    private final long reportAfterSamples;
    private final AtomicLong samplesNb = new AtomicLong(0L);

    public SamplingAggregatingProfiler(AgentConfig agentConfig, Reporter reporter) {
        super(agentConfig, reporter);

        this.reportingIntervalMs = Math.max(1, profilerConfig.getIntOrDefault("reportingMs", 10000));
        this.reportAfterSamples = Math.max(1, reportingIntervalMs / profilingIntervalMs);
        System.out.println("Will sample every " + profilingIntervalMs + " ms and report every " + reportingIntervalMs + " ms");
    }

    public abstract void start(long startTimeMs) throws Exception;

    @Override
    public void stop() throws Exception {
        super.stop();               // will sample
        this.reportInternal();      // report on stop to avoid missing values
    }

    public abstract void stop(long stopTimeMs, long deltaLastSampleMs) throws Exception;

    public abstract void sample(long sampleTimeMs, long deltaLastSampleMs) throws Exception;

    @Override
    protected void sampleInternal(long sampleTimeMs, long deltaLastSampleMs) throws Exception {
        super.sampleInternal(sampleTimeMs, deltaLastSampleMs);
        // if we have made more than a given number of samples, report the values to avoid overwhelming the reporter
        // and allow for aggregation to reduce reporting volume
        samplesNb.incrementAndGet();
        if (samplesNb.compareAndSet(reportAfterSamples, 0L)) report();
    }

    private void reportInternal() {
        try {
            report();
        }
        catch (Exception e) {
            System.err.println("Exception thrown while reporting");
            e.printStackTrace();
        }
    }

    public abstract void report() throws Exception;
}
