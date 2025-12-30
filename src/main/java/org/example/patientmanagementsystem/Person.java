package org.example.patientmanagementsystem;

public abstract class Person {
    private String tcNo;
    private String name;
    private String surname;
    private String gender;
    private int age;

    public Person(String tcNo, String name, String surname, String gender, int age) {
        this.tcNo = tcNo;
        this.name = name;
        this.surname = surname;
        this.gender = gender;
        this.age = age;
    }

    // Encapsulation (Getter/Setter)
    public String getTcNo() { return tcNo; }
    public String getName() { return name; }
    public String getSurname() { return surname; }
    public String getGender() { return gender; }
    public int getAge() { return age; }

    // --- SETTER METOTLARI (DÜZENLEME İÇİN GEREKLİ) ---
    public void setTcNo(String tcNo) { this.tcNo = tcNo; }
    public void setName(String name) { this.name = name; }
    public void setSurname(String surname) { this.surname = surname; }
    public void setGender(String gender) { this.gender = gender; }
    public void setAge(int age) { this.age = age; }

    // Polimorfizm için ortak bir metot (PDF gereksinimi: Overriding)
    public abstract String getRole();
}