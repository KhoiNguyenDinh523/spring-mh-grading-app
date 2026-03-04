package com.mhsolution.grading.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Assignment — a task/problem created by Admin.
 * Has a title, description, deadline, and a status lifecycle.
 * One Assignment can have many Solutions (one per Applicant).
 *
 * NOTE: If upgrading from a previous version, run the following SQL before
 * restarting the application to avoid NOT NULL constraint conflicts:
 *   DROP TABLE IF EXISTS solutions;
 *   DROP TABLE IF EXISTS assignments;
 * Spring Boot (ddl-auto=update) will recreate them correctly on next startup.
 */
@Entity
@Table(name = "assignments")
@Getter
@Setter
@NoArgsConstructor
public class Assignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(nullable = false)
    private LocalDateTime deadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssignmentStatus status = AssignmentStatus.PENDING;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // One assignment can have many solutions submitted by different applicants
    @OneToMany(mappedBy = "assignment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Solution> solutions = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    /** Convenience: is this assignment currently active (ONGOING and not past deadline)? */
    public boolean isEffectivelyOngoing() {
        return status == AssignmentStatus.ONGOING
                && deadline != null
                && LocalDateTime.now().isBefore(deadline);
    }

    /** Convenience: display status label including auto-expired check */
    public AssignmentStatus getEffectiveStatus() {
        if (status == AssignmentStatus.ONGOING && deadline != null && LocalDateTime.now().isAfter(deadline)) {
            return AssignmentStatus.EXPIRED;
        }
        return status;
    }
}
