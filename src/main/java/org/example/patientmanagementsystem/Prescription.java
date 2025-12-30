package org.example.patientmanagementsystem;

import java.time.LocalDate;
import java.util.List;

public class Prescription {
    private LocalDate startDate; // Başlangıç Tarihi
    private LocalDate endDate;   // Bitiş Tarihi
    private List<String> medicines;
    private String doctorNote;

    public Prescription(LocalDate startDate, LocalDate endDate, List<String> medicines, String doctorNote) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.medicines = medicines;
        this.doctorNote = doctorNote;
    }

    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public List<String> getMedicines() { return medicines; }
    public String getDoctorNote() { return doctorNote; }
}