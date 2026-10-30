package de.openshopfront.platform.commerce;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Enables maintenance jobs for pending commerce operations. */
@Configuration
@EnableScheduling
public class CommerceSchedulingConfiguration {}
