package me.florianschmidt.replication.baseline.model;

// TODO: Add empty constructor
public class Location {
	public final double latitude;
	public final double longitude;

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
