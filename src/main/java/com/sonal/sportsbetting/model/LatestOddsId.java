package com.sonal.sportsbetting.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class LatestOddsId implements Serializable {

    @Column(name = "event_id", nullable = false, length = 64)
    private String eventId;

    @Column(name = "selection", nullable = false, length = 64)
    private String selection;

    public LatestOddsId() {
    }

    public LatestOddsId(String eventId, String selection) {
        this.eventId = eventId;
        this.selection = selection;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getSelection() {
        return selection;
    }

    public void setSelection(String selection) {
        this.selection = selection;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        LatestOddsId that = (LatestOddsId) o;
        return Objects.equals(eventId, that.eventId) && Objects.equals(selection, that.selection);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId, selection);
    }
}
