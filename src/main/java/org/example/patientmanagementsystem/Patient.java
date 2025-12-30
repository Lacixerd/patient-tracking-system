package org.example.patientmanagementsystem;

import java.util.ArrayList;
import java.util.List;

public class Patient {
    private String tcNo;
    private String name;
    private String surname;
    private String gender;
    private int age;
    private String bloodGroup;
    private DiseaseTemplate disease;
    private List<HealthRecord> history;
    private List<Prescription> prescriptions;

    public Patient(String tcNo, String name, String surname, String gender, int age, String bloodGroup, DiseaseTemplate disease) {
        this.tcNo = tcNo;
        this.name = name;
        this.surname = surname;
        this.gender = gender;
        this.age = age;
        this.bloodGroup = bloodGroup;
        this.disease = disease;
        this.history = new ArrayList<>();
        this.prescriptions = new ArrayList<>();
    }

    // Getter ve Setterlar
    public String getTcNo() { return tcNo; }
    public void setTcNo(String tcNo) { this.tcNo = tcNo; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSurname() { return surname; }
    public void setSurname(String surname) { this.surname = surname; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public String getBloodGroup() { return bloodGroup; }
    public void setBloodGroup(String bloodGroup) { this.bloodGroup = bloodGroup; }

    public DiseaseTemplate getDisease() { return disease; }
    public void setDisease(DiseaseTemplate disease) { this.disease = disease; }
    public String getDiseaseName() { return disease.getName(); }

    public List<HealthRecord> getHistory() { return history; }
    public void addRecord(HealthRecord record) { this.history.add(record); }

    // Reçete Metotları
    public List<Prescription> getPrescriptions() { return prescriptions; }
    public void addPrescription(Prescription p) { this.prescriptions.add(p); }
}