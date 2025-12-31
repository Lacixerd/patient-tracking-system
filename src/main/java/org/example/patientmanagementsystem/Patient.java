package org.example.patientmanagementsystem;

import java.util.ArrayList;
import java.util.List;

public class Patient extends Person {
    private String bloodGroup;
    private DiseaseTemplate disease;
    private List<HealthRecord> history;
    private List<Prescription> prescriptions;

    public Patient(String tcNo, String name, String surname, String gender, int age, String bloodGroup,
            DiseaseTemplate disease) {
        super(tcNo, name, surname, gender, age);
        this.bloodGroup = bloodGroup;
        this.disease = disease;
        this.history = new ArrayList<>();
        this.prescriptions = new ArrayList<>();
    }

    // Person sınıfındaki abstract metodu override ediyoruz
    @Override
    public String getRole() {
        return "Hasta";
    }

    // Patient'a özgü Getter ve Setterlar
    public String getBloodGroup() {
        return bloodGroup;
    }

    public void setBloodGroup(String bloodGroup) {
        this.bloodGroup = bloodGroup;
    }

    public DiseaseTemplate getDisease() {
        return disease;
    }

    public void setDisease(DiseaseTemplate disease) {
        this.disease = disease;
    }

    public String getDiseaseName() {
        return disease.getName();
    }

    public List<HealthRecord> getHistory() {
        return history;
    }

    public void addRecord(HealthRecord record) {
        this.history.add(record);
    }

    // Reçete Metotları
    public List<Prescription> getPrescriptions() {
        return prescriptions;
    }

    public void addPrescription(Prescription p) {
        this.prescriptions.add(p);
    }
}
