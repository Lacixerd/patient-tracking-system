package org.example.patientmanagementsystem;

import java.time.LocalDate;
import java.util.Map;

public class HealthRecord {
    private LocalDate date;
    private String note;
    private Map<String, String> values;

    public HealthRecord(LocalDate date, String note, Map<String, String> values) {
        this.date = date;
        this.note = note;
        this.values = values;
    }

    // Getterlar
    public LocalDate getDate() { return date; }
    public String getNote() { return note; }
    public Map<String, String> getValues() { return values; }

    // Yardımcı metod
    public String getValue(String key) {
        return values.getOrDefault(key, "-");
    }

    // --- SETTER METOTLARI (DÜZENLEME İÇİN) ---
    public void setDate(LocalDate date) { this.date = date; }
    public void setNote(String note) { this.note = note; }
    // Values map'i zaten referans olduğu için getter ile alıp put yapabiliriz, setter'a gerek yok.
}