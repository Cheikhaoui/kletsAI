package com.kletsai.chat;

import com.kletsai.ai.TutorClient;
import com.kletsai.ai.TutorReply;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** Returns queued replies instead of calling Claude, and records what it was asked. */
public class FakeTutorClient implements TutorClient {

    public record Call(String scenarioInstructions, List<Turn> history, String learnerText) {
    }

    private final Deque<Object> replies = new ArrayDeque<>();
    public final List<Call> calls = new ArrayList<>();

    public void willReply(TutorReply reply) {
        replies.add(reply);
    }

    public void willFail(RuntimeException e) {
        replies.add(e);
    }

    public void reset() {
        replies.clear();
        calls.clear();
    }

    @Override
    public TutorReply respond(String scenarioInstructions, List<Turn> history, String learnerText) {
        calls.add(new Call(scenarioInstructions, List.copyOf(history), learnerText));
        Object next = replies.isEmpty()
                ? new TutorReply(List.of(new TutorReply.Line("Hallo!", "Hello!")), List.of())
                : replies.poll();
        if (next instanceof RuntimeException e) {
            throw e;
        }
        return (TutorReply) next;
    }
}
