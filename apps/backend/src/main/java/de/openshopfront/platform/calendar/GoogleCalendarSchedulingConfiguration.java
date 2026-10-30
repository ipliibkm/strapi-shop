package de.openshopfront.platform.calendar;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Enables Calendar synchronization and booking-event delivery schedules. */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "platform.google.enabled", havingValue = "true")
class GoogleCalendarSchedulingConfiguration {}
