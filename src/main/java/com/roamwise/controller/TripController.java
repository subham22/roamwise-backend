package com.roamwise.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.roamwise.dto.*;
import com.roamwise.dto.ai_response.GeneratedItinerary;
import com.roamwise.entity.*;
import com.roamwise.repository.*;
import com.roamwise.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;
    private final JobRepository jobRepository;
    private final DayRepository dayRepository;
    private final WeatherService weatherService;
    private final EmailService emailService;
    private final PdfExportService pdfExportService;
    private final IcsExportService icsExportService;
    private final ItineraryGenerationService itineraryGenerationService;
    private final OpenAiService openAiService;
    private final ObjectMapper objectMapper;
    private final TripRepository tripRepository;
    private final ReplanProposalRepository replanProposalRepository;
    private final AccommodationRepository accommodationRepository;
    private final PlacesService placesService;
    private final ActivityRepository activityRepository;

    @PostMapping("/{tripId}/replan")
    public ResponseEntity<ReplanProposalResponse> proposeReplan(
            @PathVariable Integer tripId,
            @RequestBody ReplanRequest request,
            @AuthenticationPrincipal User currentUser) {

        TripResponse tripResponse = tripService.getTripById(tripId, currentUser.getId());
    String prompt =
        itineraryGenerationService.buildReplanPrompt(
            tripResponse, request.getBudgetDelta(), request.getInterest());
        String rawJson = openAiService.generateItinerary(prompt);

        GeneratedItinerary generatedItinerary = objectMapper.readValue(rawJson, GeneratedItinerary.class);
        System.out.println(generatedItinerary);
        List<String> diff = itineraryGenerationService.diffItineraries(tripResponse, generatedItinerary);
        List<PlaceResult> places = placesService.getPlacesForItinerary(tripResponse.getDestination(), request.getInterest());
        ReplanProposal proposal = new ReplanProposal();
        proposal.setTrip(tripRepository.findById(tripId).orElseThrow());
        proposal.setProposedJson(rawJson);
        proposal.setDiffSummary(String.join("\n", diff));
        proposal.setCreatedAt(LocalDateTime.now());
        proposal.setPlacesJson(objectMapper.writeValueAsString(places));
        ReplanProposal saved = replanProposalRepository.save(proposal);

        return ResponseEntity.ok(new ReplanProposalResponse(saved.getId(), diff));

    }

    @PostMapping("/replan/{proposalId}/confirm")
    public ResponseEntity<Void> confirmReplan(@PathVariable Integer proposalId, @AuthenticationPrincipal User currentUser) throws JsonProcessingException {
        tripService.confirmReplan(proposalId, currentUser.getId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/shared/{shareToken}")
    public ResponseEntity<TripResponse> getSharedTrip(@PathVariable String shareToken) {
        return ResponseEntity.ok(tripService.getSharedTrip(shareToken));
    }



    @GetMapping("/{tripId}/export/pdf")
    public ResponseEntity<byte[]> exportPdf(@PathVariable Integer tripId, @AuthenticationPrincipal User currentUser) throws IOException {
        TripResponse trip = tripService.getTripById(tripId, currentUser.getId());

        byte[] pdf = pdfExportService.generatePdf(trip);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=trip.pdf")
                .body(pdf);
    }

    @GetMapping("/{tripId}/export/ics")
    public ResponseEntity<String> exportCalendar(@PathVariable Integer tripId, @AuthenticationPrincipal User currentUser) throws IOException {
        TripResponse trip = tripService.getTripById(tripId, currentUser.getId());

        String ics = icsExportService.generateIcs(trip);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/calendar"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=trip.ics")
                .body(ics);
    }

    @PostMapping("/{tripId}/share")
    public ResponseEntity<Map<String, String>> shareTrip(@PathVariable Integer tripId, @RequestBody ShareRequest request, @AuthenticationPrincipal User currentUser) {
        String shareUrl = tripService.generateShareLink(tripId, currentUser.getId());
        if (request.getRecipientEmail() != null) {
            TripResponse trip = tripService.getTripById(tripId, currentUser.getId());
            emailService.sendShareNotification(request.getRecipientEmail(), trip.getDestination(), shareUrl);
        }
        return ResponseEntity.ok(Map.of("shareUrl", shareUrl));
    }

    @PostMapping
    public ResponseEntity<TripResponse> createTrip(@Valid @RequestBody CreateTripRequest request) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        TripResponse tripResponse = tripService.createTrip(request, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(tripResponse);

    }

    @PostMapping("/generate")
    public  ResponseEntity<Integer> createAITrip(@Valid @RequestBody GenerateItineraryRequest request) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Integer jobId = tripService.startAIGeneration(request, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(jobId);
    }

    @GetMapping("/{tripId}/days/{dayId}/weather")
    public ResponseEntity<Map<String, String>> getDayWeather(@PathVariable Integer tripId, @PathVariable Integer dayId) {
        Day day = dayRepository.findById(dayId).orElseThrow(() -> new RuntimeException("Day not found"));
        String weather = weatherService.getWeather(day.getDayDate(), day.getTrip().getDestination());
        return ResponseEntity.ok(Map.of("weather", weather));
    }

    @GetMapping
    public ResponseEntity<List<TripResponse>> getAllTrips(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok().body(tripService.getMyTrips(user.getId()));
    }

    @GetMapping("{tripId}")
    public ResponseEntity<TripResponse> getTripById(@PathVariable Integer tripId, @AuthenticationPrincipal User currentUser){
        return ResponseEntity.ok().body(tripService.getTripById(tripId, currentUser.getId()));
    }


}
