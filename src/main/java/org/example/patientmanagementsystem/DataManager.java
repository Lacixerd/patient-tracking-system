package org.example.patientmanagementsystem;

import java.io.*;
import java.time.LocalDate;
import java.util.*;

public class DataManager {

    private static final String DISEASES_FILE = "diseases.csv";
    private static final String PATIENTS_FILE = "patients.csv";
    private static final String RECORDS_FILE = "records.csv";
    private static final String PRESCRIPTIONS_FILE = "prescriptions.csv";
    private static final String DELIMITER = ";";

    public static void saveData(List<DiseaseTemplate> diseases, List<Patient> patients) {
        saveDiseases(diseases);
        savePatients(patients);
        saveRecords(patients);
        savePrescriptions(patients);
    }

    private static void savePatients(List<Patient> patients) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(PATIENTS_FILE))) {
            for (Patient p : patients) {
                String line = String.join(DELIMITER,
                        p.getTcNo(), p.getName(), p.getSurname(), p.getGender(),
                        String.valueOf(p.getAge()), p.getBloodGroup(), p.getDiseaseName());
                writer.write(line); writer.newLine();
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    private static void saveRecords(List<Patient> patients) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(RECORDS_FILE))) {
            for (Patient p : patients) {
                for (HealthRecord r : p.getHistory()) {
                    StringBuilder values = new StringBuilder();
                    for (Map.Entry<String, String> entry : r.getValues().entrySet()) {
                        values.append(entry.getKey()).append(":").append(entry.getValue()).append("|");
                    }
                    String safeNote = r.getNote().replace(";", "").replace("\n", " ");
                    writer.write(p.getTcNo() + DELIMITER + r.getDate() + DELIMITER + safeNote + DELIMITER + values);
                    writer.newLine();
                }
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    private static void savePrescriptions(List<Patient> patients) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(PRESCRIPTIONS_FILE))) {
            for (Patient p : patients) {
                for (Prescription pr : p.getPrescriptions()) {
                    String meds = String.join(",", pr.getMedicines());
                    String safeNote = pr.getDoctorNote().replace(";", "").replace("\n", " ");
                    writer.write(p.getTcNo() + DELIMITER + pr.getStartDate() + DELIMITER + pr.getEndDate() + DELIMITER + meds + DELIMITER + safeNote);
                    writer.newLine();
                }
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    public static List<DiseaseTemplate> loadDiseases() {
        List<DiseaseTemplate> list = new ArrayList<>();
        File file = new File(DISEASES_FILE);
        if (!file.exists()) return list;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(DELIMITER);
                if (parts.length >= 2) {
                    List<String> params = Arrays.asList(parts[1].split(","));
                    List<String> meds = new ArrayList<>();
                    if (parts.length > 2 && !parts[2].isEmpty()) {
                        meds = Arrays.asList(parts[2].split(","));
                    }

                    Map<String, String> thresholds = new HashMap<>();
                    if (parts.length > 3 && !parts[3].isEmpty()) {
                        String[] pairs = parts[3].split(",");
                        for (String pair : pairs) {
                            String[] kv = pair.split("=");
                            if (kv.length == 2) {
                                thresholds.put(kv[0], kv[1]);
                            }
                        }
                    }
                    list.add(new DiseaseTemplate(parts[0], params, meds, thresholds));
                }
            }
        } catch (IOException e) { e.printStackTrace(); }
        return list;
    }

    private static void saveDiseases(List<DiseaseTemplate> diseases) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(DISEASES_FILE))) {
            for (DiseaseTemplate dt : diseases) {
                String params = String.join(",", dt.getParameters());
                String meds = String.join(",", dt.getDefaultMedicines());

                StringBuilder thresholdsBuilder = new StringBuilder();
                for (Map.Entry<String, String> entry : dt.getCriticalThresholds().entrySet()) {
                    thresholdsBuilder.append(entry.getKey()).append("=").append(entry.getValue()).append(",");
                }
                String thresholds = thresholdsBuilder.toString();
                if (thresholds.length() > 0) thresholds = thresholds.substring(0, thresholds.length() - 1);

                writer.write(dt.getName() + DELIMITER + params + DELIMITER + meds + DELIMITER + thresholds);
                writer.newLine();
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    public static List<Patient> loadPatients(List<DiseaseTemplate> templates) {
        List<Patient> list = new ArrayList<>();
        File file = new File(PATIENTS_FILE);
        if (!file.exists()) return list;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(DELIMITER);
                if (parts.length >= 7) {
                    String dName = parts[6];
                    DiseaseTemplate dt = templates.stream().filter(t -> t.getName().equals(dName)).findFirst().orElse(templates.get(0));
                    list.add(new Patient(parts[0], parts[1], parts[2], parts[3], Integer.parseInt(parts[4]), parts[5], dt));
                }
            }
        } catch (IOException e) { e.printStackTrace(); }
        return list;
    }

    public static void loadRecords(List<Patient> patients) {
        File file = new File(RECORDS_FILE);
        if (!file.exists()) return;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(DELIMITER);
                if (parts.length >= 4) {
                    String tc = parts[0];
                    Patient p = patients.stream().filter(pt -> pt.getTcNo().equals(tc)).findFirst().orElse(null);
                    if (p != null) {
                        Map<String, String> map = new HashMap<>();
                        if(!parts[3].isEmpty()) {
                            for(String s : parts[3].split("\\|")) {
                                String[] kv = s.split(":");
                                if(kv.length==2) map.put(kv[0], kv[1]);
                            }
                        }
                        p.addRecord(new HealthRecord(LocalDate.parse(parts[1]), parts[2], map));
                    }
                }
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    public static void loadPrescriptions(List<Patient> patients) {
        File file = new File(PRESCRIPTIONS_FILE);
        if (!file.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(DELIMITER);
                if (parts.length >= 5) {
                    String tc = parts[0];
                    Patient p = patients.stream().filter(pt -> pt.getTcNo().equals(tc)).findFirst().orElse(null);
                    if (p != null) {
                        List<String> meds = new ArrayList<>();
                        if (!parts[3].isEmpty()) {
                            meds = Arrays.asList(parts[3].split(","));
                        }
                        LocalDate start = LocalDate.parse(parts[1]);
                        LocalDate end = LocalDate.parse(parts[2]);
                        p.addPrescription(new Prescription(start, end, meds, parts[4]));
                    }
                }
            }
        } catch (IOException e) { e.printStackTrace(); }
    }
}