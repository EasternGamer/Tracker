/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package easterngamer;

import discord4j.core.object.DiscordObject;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.MessageEditSpec;
import discord4j.rest.util.Color;
import java.time.Duration;
import java.util.function.Consumer;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 *
 * @author crysi
 */
public class Tracker {

    private long max;
    private double position = 0;
    /**
     * BAR_START: First is the start empty emoji, second is the animated start emoji and lastly is the filled emoji.
     */
    private static final String[] BAR_START = {"<:SE:711968978989219841>", "<a:SA:711971797028962396>", "<:SF:711969021880303686>"};
    /**
     * BAR_MIDDLE: First is the middle empty emoji, second is the animated middle emoji and lastly is the filled emoji.
     */
    private static final String[] BAR_MIDDLE = {"<:ME:711969359064727594>", "<a:MA:711987534384726100>", "<:MF:711969377280327711>"};
    /**
     * BAR_END: First is the end empty emoji, second is the animated end emoji and lastly is the filled emoji.
     */
    private static final String[] BAR_END = {"<:EE:711969411803643955>", "<a:EA:711971755086053476>", "<:EF:711969435744731138>"};
    
    /**
     * Holds the percentage bar in the form of a string. 
     */
    private String percentBar = "";

    /**
     * Used for when there is a known max.
     * @param max the known number of entries to count.
     */
    public Tracker(int max) {
        this.max = max;
    }

    /**
     * Used for when there is a known max.
     * @param operation is subscribed to to get the count, it is important this doesn't relate to any form of interaction where a response from Discord is required. 
     */
    public Tracker(Flux<?> operation) {
        operation.count()
                .subscribe(count -> max = count);
    }

    /**
     * @param increment is how much has happened
     * @return the position with the new increment.
     */
    public double increasePosition(int increment) {
        position += increment;
        return position;
    }

    public long getMax() {
        return max;
    }

    public void setMax(long max) {
        this.max = max;
    }

    /**
     * Logic for getting the string representation of the progress bar, in it's static RAW form, with ";" seperators. 
     */
    private String getStaticPercentBarRaw() {
        StringBuilder percentBarBuilder = new StringBuilder();
        double percentage = getPercentage();
        if (percentage >= 5) {
            percentBarBuilder.append(BAR_START[2]);
        } else {
            percentBarBuilder.append(BAR_START[0]);
        }
        percentBarBuilder.append(";");
        for (int i = 1; i < 10; i++) {
            if (percentage < i * 10) {
                percentBarBuilder.append(BAR_MIDDLE[0]);
            } else {
                percentBarBuilder.append(BAR_MIDDLE[2]);
            }
            percentBarBuilder.append(";");
        }
        if (percentage >= 95) {
            percentBarBuilder.append(BAR_END[2]);
        } else {
            percentBarBuilder.append(BAR_END[0]);
        }
        return percentBarBuilder.toString();
    }

    /**
     * Logic for getting the string representation of the progress bar, in its static form.
     * @return 
     */
    public String getStaticPercentBar() {
        return StringUtils.remove(getStaticPercentBarRaw(), ";");
    }

    /**
     * Logic for getting the string representation of the progress bar, in its dynamic form.
     * @return the Discord-passable form.
     */
    public String getDynamicPercentBar() {
        String[] percentBarBuilder = getStaticPercentBarRaw().split(";");
        for (int i = percentBarBuilder.length - 1; i >= 0; i--) {
            if (percentBarBuilder[i].equals(BAR_END[2])) {
                percentBarBuilder[i] = BAR_END[1];
                break;
            } else if (percentBarBuilder[i].equals(BAR_MIDDLE[2])) {
                percentBarBuilder[i] = BAR_MIDDLE[1];
                break;
            } else if (percentBarBuilder[i].equals(BAR_START[2])) {
                percentBarBuilder[i] = BAR_START[1];
                break;
            }
        }
        StringBuilder bar = new StringBuilder();
        for (String barSegment : percentBarBuilder) {
            bar.append(barSegment);
        }
        return bar.toString();
    }

    /**
     * Logic for getting the the message edit spec for a message containing the percentage bar, including the logic for deciding what type of progress bar is needed.
     * @return the Consumer of the spec.
     */ 
    public Consumer<MessageEditSpec> getStandardMessageContent() {
        Consumer<MessageEditSpec> spec = messageSpec -> {
            messageSpec.setContent("__**Tracker v1.0**__");
            messageSpec.setEmbed(embedSpec -> {
                embedSpec.setColor(Color.of(3553598));

                String staticPercentBar = getStaticPercentBar();
                if (staticPercentBar.equals(percentBar)) {
                    embedSpec.setDescription("**Progress: ** ``" + getPosition() + "/" + getMax() + "``\n" + staticPercentBar);
                } else {
                    embedSpec.setDescription("**Progress: ** ``" + getPosition() + "/" + getMax() + "``\n" + getDynamicPercentBar());
                }
                percentBar = staticPercentBar;
            });
        };
        return spec;
    }

    /**
     * @return the position of the current 
    */
    public double getPosition() {
        return position;
    }

    /**
     * @param messageChannel the channel the message is to be sent in.
     * @return the message created, mostly not needed.
    */
    public Mono<Message> createTrackerMessage(Mono<MessageChannel> messageChannel) {
        return messageChannel.flatMap(channel -> channel.createMessage((t) -> {
            t.setContent("Initiating...");
        }));
    }

    /**
     * @return the percentage of the progress to two decimal places.
    */
    public double getPercentage() {
        return Math.round((position / max) * 10000.0) / 100.0;
    }

    /**
     * Uses the default buffering time of 7 seconds and the animation edit time of 1500ms.
     * @param operation the operation which needs to be buffered for to count the progress. Must not be any form of void Flux.
     * @param operationToCount the operation which is used to count the number of <b>possible</b> operations.
     * @param channel the channel in which the progress bar will be sent.
     * @return The message instances which were edited.
     */
    public static final Flux<Message> of(Flux<? extends DiscordObject> operation, Flux<?> operationToCount, Mono<MessageChannel> channel) {
            return of(operation, operationToCount, channel, Duration.ofSeconds(7), Duration.ofMillis(1500));
    }
    /**
     * Uses a custom buffer length and a custom animation edit time.
     * @param operation the operation which needs to be buffered for to count the progress. Must not be any form of void Flux.
     * @param operationToCount the operation which is used to count the number of <b>possible</b> operations.
     * @param channel the channel in which the progress bar will be sent.
     * @param durationOfBuffer the duration between each buffer. Higher values mean <b>theorically</b> less precise results, lower values generally mean being more precise, however, can also result in rate-limiting, making it far less precise. 
     * @return The message instances which were edited.
     */
    public static final Flux<Message> of(Flux<? extends DiscordObject> operation, Flux<?> operationToCount, Mono<MessageChannel> channel, Duration durationOfBuffer) {
            return of(operation, operationToCount, channel, durationOfBuffer, Duration.ofMillis(1500));
    }
    
    /**
     * Uses a custom buffer length and the animation edit time of 1500ms.
     * @param operation the operation which needs to be buffered for to count the progress. Must not be any form of void Flux.
     * @param operationToCount the operation which is used to count the number of <b>possible</b> operations.
     * @param channel the channel in which the progress bar will be sent.
     * @param durationOfBuffer the duration between each buffer. Higher values mean <b>theorically</b> less precise results, lower values generally mean being more precise, however, can also result in rate-limiting, making it far less precise. 
     * @param durationOfAnimation the duration between editing the message with the animated and static versions. This should be set to the duration of the animation of the animated emojis.
     * @return The message instances which were edited.
     */
    public static final Flux<Message> of(Flux<? extends DiscordObject> operation, Flux<?> operationToCount, Mono<MessageChannel> channel, Duration durationOfBuffer, Duration durationOfAnimation) {
            Tracker tracker = new Tracker(operationToCount);
            return tracker.createTrackerMessage(channel)
                    .flatMap(message -> message.edit(tracker.getStandardMessageContent()))
                    .flatMapMany(message -> {
                        return operation.buffer(durationOfBuffer)
                                .flatMap(list -> {
                                    tracker.increasePosition(list.size());
                                    return message
                                            .edit(tracker.getStandardMessageContent())
                                            .delayElement(durationOfAnimation)
                                            .flatMap(editedMessage -> editedMessage.edit(tracker.getStandardMessageContent()));
                                })
                                .doOnComplete(() -> {
                                    message.edit(spec -> spec
                                            .setEmbed(/*What you want when it is completed*/)).subscribe();
                                });
                    });
    }
}
