package org.example.patientmanagementsystem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//Hastalıkların kaydı sırasındaki esneklik için oluşturulan hastalık taslağı
public class DiseaseTemplate {
    private String name;
    private List<String> parameters;
    private List<String> defaultMedicines;
    private Map<String, String> criticalThresholds; // String yaptık (Örn: ">120")

    public DiseaseTemplate(String name, List<String> parameters, List<String> defaultMedicines, Map<String, String> criticalThresholds) {
        this.name = name;
        this.parameters = parameters != null ? new ArrayList<>(parameters) : new ArrayList<>();
        this.defaultMedicines = defaultMedicines != null ? new ArrayList<>(defaultMedicines) : new ArrayList<>();
        this.criticalThresholds = criticalThresholds != null ? new HashMap<>(criticalThresholds) : new HashMap<>();
    }

    public DiseaseTemplate(String name, List<String> parameters, List<String> defaultMedicines) {
        this(name, parameters, defaultMedicines, new HashMap<>());
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<String> getParameters() { return parameters; }
    public void setParameters(List<String> parameters) {
        this.parameters = parameters != null ? new ArrayList<>(parameters) : new ArrayList<>();
    }

    public List<String> getDefaultMedicines() { return defaultMedicines; }
    public void setDefaultMedicines(List<String> defaultMedicines) {
        this.defaultMedicines = defaultMedicines != null ? new ArrayList<>(defaultMedicines) : new ArrayList<>();
    }

    public Map<String, String> getCriticalThresholds() { return criticalThresholds; }
    public void setCriticalThresholds(Map<String, String> criticalThresholds) {
        this.criticalThresholds = criticalThresholds != null ? new HashMap<>(criticalThresholds) : new HashMap<>();
    }

    public String getThreshold(String param) {
        return criticalThresholds.get(param);
    }

    @Override
    public String toString() { return name; }
}