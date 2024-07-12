package com.guilherme.planner.trip;

import com.guilherme.planner.activity.ActivityData;
import com.guilherme.planner.activity.ActivityRequest;
import com.guilherme.planner.activity.ActivityResponse;
import com.guilherme.planner.activity.ActivityService;
import com.guilherme.planner.link.LinkData;
import com.guilherme.planner.link.LinkRequest;
import com.guilherme.planner.link.LinkResponse;
import com.guilherme.planner.link.LinkService;
import com.guilherme.planner.participant.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/trips")
public class TripController {

    @Autowired
    private ParticipantService participantService;

    @Autowired
    private ActivityService activityService;

    @Autowired
    private LinkService linkService;

    @Autowired
    private TripRepository tripRepository;

    @PostMapping
    public ResponseEntity<TripCreateResponse> createTrip(@RequestBody TripRequest payload) {
        Trip newTrip = new Trip(payload);

        this.tripRepository.save(newTrip);

        this.participantService.registerParticipantsToEvent(payload.emails_to_invite(), newTrip);

        return ResponseEntity.ok(new TripCreateResponse(newTrip.getId()));
    };

    @GetMapping("/{id}")
    public ResponseEntity<Trip> getTripDetails(@PathVariable UUID id){
        Optional<Trip> trip = this.tripRepository.findById(id);

        return trip.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    };

    @PutMapping("/{id}")
    public ResponseEntity<Trip> updateTrip(@PathVariable UUID id, @RequestBody TripRequest payload){
        Optional<Trip> trip = this.tripRepository.findById(id);

        if(trip.isPresent()){
            Trip rawTrip = trip.get();
            rawTrip.setEndsAt(LocalDateTime.parse(payload.ends_at(), DateTimeFormatter.ISO_DATE_TIME));
            rawTrip.setStartsAt(LocalDateTime.parse(payload.starts_at(), DateTimeFormatter.ISO_DATE_TIME));
            rawTrip.setDestination(payload.destination());

            this.tripRepository.save(rawTrip);
            return ResponseEntity.ok(rawTrip);
        }
        return ResponseEntity.notFound().build();
    };

    @GetMapping("/{id}/confirm")
    public ResponseEntity<Trip> confirmTrip(@PathVariable UUID id){
        Optional<Trip> trip = this.tripRepository.findById(id);

        if(trip.isPresent()){
            Trip rawTrip = trip.get();
            rawTrip.setIsConfirmed(true);

            this.tripRepository.save(rawTrip);
            this.participantService.triggerConfirmationEmailToParticipants(id);
            return ResponseEntity.ok(rawTrip);
        }
        return ResponseEntity.notFound().build();
    };

    @PostMapping("/{id}/invite")
    public ResponseEntity<ParticipantCreateResponse> inviteParticipant(@PathVariable UUID id, @RequestBody ParticipantRequest payload) {
        Optional<Trip> trip = this.tripRepository.findById(id);

        if(trip.isPresent()){
            Trip rawTrip = trip.get();

            ParticipantCreateResponse participantResponse = this.participantService.registerParticipantToEvent(payload.email(), rawTrip);

            if(rawTrip.getIsConfirmed()){
                this.participantService.triggerConfirmationEmailToParticipant(payload.email());
            }
            return ResponseEntity.ok(participantResponse);
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/{id}/activities")
    public ResponseEntity<ActivityResponse> registerActivity(@PathVariable UUID id, @RequestBody ActivityRequest payload) {
        Optional<Trip> trip = this.tripRepository.findById(id);

        if(trip.isPresent()){
            Trip rawTrip = trip.get();

            ActivityResponse activityResponse = this.activityService.registerActivity(payload, rawTrip);

            return ResponseEntity.ok(activityResponse);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}/activities")
    public ResponseEntity<List<ActivityData>> getAllActivities(@PathVariable UUID id){

        List<ActivityData> activitiesList = this.activityService.getAllActivitiesFromId(id);

        return ResponseEntity.ok(activitiesList);
    }

    @GetMapping("/{id}/participants")
    public ResponseEntity<List<ParticipantData>> getAllParticipants(@PathVariable UUID id){

        List<ParticipantData> participantsList = this.participantService.getAllParticipantsFromTrip(id);

        return ResponseEntity.ok(participantsList);
    }

    @PostMapping("/{id}/links")
    public ResponseEntity<LinkResponse> registerLinks(@PathVariable UUID id, @RequestBody LinkRequest payload) {
        Optional<Trip> trip = this.tripRepository.findById(id);

        if(trip.isPresent()){
            Trip rawTrip = trip.get();

            LinkResponse linkResponse = this.linkService.registerLink(payload, rawTrip);

            return ResponseEntity.ok(linkResponse);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}/links")
    public ResponseEntity<List<LinkData>> getAllLinks(@PathVariable UUID id){

        List<LinkData> linkList = this.linkService.getAllLinksFromId(id);

        return ResponseEntity.ok(linkList);
    }
}
