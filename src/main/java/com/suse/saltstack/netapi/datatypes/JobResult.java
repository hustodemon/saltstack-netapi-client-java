package com.suse.saltstack.netapi.datatypes;

import java.util.Map;

/**
 * Representation of previously run job with its result {@link Job}
 */
public class JobResult {

    private Job job;
    private Map<String, Object> results;

    public JobResult(Job job, Map<String, Object> results) {
        this.job = job;
        this.results = results;
    }

    public Job getJob() {
        return job;
    }

    /**
     * Gets per-minion results of the job.
     * @return map containing results of the job keyed by minion name.
     */
    public Map<String, Object> getResults() {
        return results;
    }
}
