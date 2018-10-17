package me.florianschmidt.replication.baseline.model;

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;

public class Location {

	@JsonProperty("Latitude")
	public double latitude;

	@JsonProperty("Longitude")
	public double longitude;

	public Location() {
	}

	public Location(double latitude, double longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
	}

	@Override
	public String toString() {
		return "Location{" +
				"latitude=" + latitude +
				", longitude=" + longitude +
				'}';
	}
}
