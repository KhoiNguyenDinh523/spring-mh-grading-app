package com.mhsolution.grading.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Solution — a file submission by an Applicant for a specific Assignment.
 *
 * Key constraints:
 *  - One Applicant can submit at most ONE Solution per Assignment
 *    (enforced by @UniqueConstraint at DB level + service-level check).
 *  - The file is stored outside the web root (UUID-named on disk).
 *  - An Admin can assign this Solution to a specific Grader.
 *  - Only the assigned Grader (or Admin) can grade this Solution.
 */
@Entity
@Table(
    name = "solutions",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_solution_assignment_uploader",
        columnNames = {"assignment_id", "uploader_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
public class Solution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The assignment (task) this solution addresses
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;

    // The applicant who uploaded this solution
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploader_id", nullable = false)
    private User uploader;

    // ---- File metadata ----
    @Column(nullable = false)
    private String originalFilename;

    @Column(nullable = false, unique = true)
    private String storedFilename;   // UUID-based filename on disk

    @Column(nullable = false)
    private String filePath;

    @Column(nullable = false, length = 20)
    private String fileExtension;

    private long fileSize;

    // Optional comment by the Applicant when submitting
    @Column(columnDefinition = "TEXT")
    private String applicantComment;

    @Column(nullable = false)
    private LocalDateTime submittedAt;

    // ---- Grader assignment (set by Admin) ----
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_grader_id")
    private User assignedGrader;   // null until Admin assigns

    // ---- Grader evaluation ----
    @Column(columnDefinition = "TEXT")
    private String graderComment;

    private Integer score;           // null until graded

    private LocalDateTime gradedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "graded_by_id")
    private User gradedBy;

    @PrePersist
    protected void onCreate() {
        this.submittedAt = LocalDateTime.now();
    }

    public boolean isGraded() {
        return score != null;
    }
}
