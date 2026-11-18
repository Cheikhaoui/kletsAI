package com.kletsai.ai;

import java.util.List;

/**
 * The only way the app talks to the AI tutor. Tests replace it with a fake, so they never call Claude.
 */
public interface TutorClient {

    /**
     * @param scenarioInstructions what the role-play is about (who the tutor plays, the setting)
     * @param history              earlier turns, oldest first, starting with the tutor's opening
     * @param learnerText          the learner's new message, or null to get the opening line
     */
    TutorReply respond(String scenarioInstructions, List<Turn> history, String learnerText);

    /** A past turn. For the tutor, {@code text} is the JSON it produced. */
    record Turn(Role role, String text) {
        public enum Role { LEARNER, TUTOR }
    }
}
