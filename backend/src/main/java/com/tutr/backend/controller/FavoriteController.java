package com.tutr.backend.controller;

import com.tutr.backend.dto.FavoriteCourse;
import com.tutr.backend.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/student/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    // Add course to favorites
    @PostMapping("/{studentId}/add/{courseId}")
    public ResponseEntity<?> addToFavorites(
            @PathVariable Long studentId,
            @PathVariable Long courseId) {
        try {
            String message = favoriteService.addToFavorites(studentId, courseId);
            return ResponseEntity.ok(message);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // Remove course from favorites by student and course
    @DeleteMapping("/{studentId}/remove/{courseId}")
    public ResponseEntity<?> removeFromFavorites(
            @PathVariable Long studentId,
            @PathVariable Long courseId) {
        try {
            String message = favoriteService.removeFromFavorites(studentId, courseId);
            return ResponseEntity.ok(message);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // Remove favorite by favorite ID
    @DeleteMapping("/remove/{favoriteId}")
    public ResponseEntity<?> removeFavoriteById(@PathVariable Long favoriteId) {
        try {
            String message = favoriteService.removeFavoriteById(favoriteId);
            return ResponseEntity.ok(message);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // Get all favorites for a student
    @GetMapping("/{studentId}")
    public ResponseEntity<?> getStudentFavorites(@PathVariable Long studentId) {
        try {
            List<FavoriteCourse> favorites = favoriteService.getStudentFavorites(studentId);
            return ResponseEntity.ok(favorites);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // Check if a course is favorited by a student
    @GetMapping("/{studentId}/check/{courseId}")
    public ResponseEntity<?> checkIsFavorite(
            @PathVariable Long studentId,
            @PathVariable Long courseId) {
        try {
            boolean isFavorite = favoriteService.isFavorite(studentId, courseId);
            return ResponseEntity.ok(isFavorite);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error checking favorite status");
        }
    }
}