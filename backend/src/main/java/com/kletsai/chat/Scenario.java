package com.kletsai.chat;

/** Role-play scenes. The instructions are appended to the tutor's system prompt under "## The scene". */
public enum Scenario {

    FREE("Vrij gesprek", "Free conversation",
            "Chat about everyday things: your day, work, hobbies, the weather.",
            """
            A relaxed everyday chat. You are a friendly Dutch neighbour. Talk about simple daily topics: \
            the learner's day, work, family, hobbies, food, the weather. Follow the learner's lead.
            """),

    TRAIN_TICKET("Een kaartje kopen", "Buying a train ticket",
            "You're at the NS ticket desk at Utrecht Centraal.",
            """
            You are a friendly NS employee at the ticket desk at Utrecht Centraal station. The learner wants to \
            travel by train. Help them buy a ticket: ask where they want to go, single or return \
            (enkele reis / retour), first or second class, and how they want to pay. You can mention the \
            platform (spoor) and departure time. Keep prices and times realistic.
            """),

    DOCTOR("Bij de dokter", "At the doctor",
            "You're at the GP (huisarts) because you don't feel well.",
            """
            You are a calm, kind huisarts (GP) in the Netherlands. The learner is your patient. Ask what the \
            problem is, where it hurts, since when, and whether they have a fever. Give simple advice and, \
            if it fits, a prescription or a follow-up appointment. Use simple body words (hoofd, buik, keel, rug).
            """),

    COLLEAGUE("Met een collega", "Talking with a colleague",
            "Coffee-machine chat with a Dutch colleague at the office.",
            """
            You are Sanne, a friendly Dutch colleague at a software company. You meet the learner at the coffee \
            machine. Make small talk: the weekend, a project or meeting, lunch plans, the commute by train or \
            bike. Use informal "je/jij".
            """);

    private final String titleNl;
    private final String titleEn;
    private final String description;
    private final String instructions;

    Scenario(String titleNl, String titleEn, String description, String instructions) {
        this.titleNl = titleNl;
        this.titleEn = titleEn;
        this.description = description;
        this.instructions = instructions;
    }

    public String titleNl() { return titleNl; }
    public String titleEn() { return titleEn; }
    public String description() { return description; }
    public String instructions() { return instructions; }
}
