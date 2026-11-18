package com.bookexchange.model;

/** A registered student (table: students). */
public class Student extends BaseEntity {
    private int id;
    private String fullName;
    private String email;
    private String collegeId;
    private String phone;
    private String passwordHash;

    public Student() { }

    public Student(String fullName, String email, String collegeId, String passwordHash) {
        this.fullName = fullName;
        this.email = email;
        this.collegeId = collegeId;
        this.passwordHash = passwordHash;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getCollegeId() { return collegeId; }
    public void setCollegeId(String collegeId) { this.collegeId = collegeId; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getFirstName() {
        String n = fullName == null ? "" : fullName.trim();
        int space = n.indexOf(' ');
        return space > 0 ? n.substring(0, space) : n;
    }

    @Override
    public String getSummary() { return "Student#" + id + " " + fullName + " (" + collegeId + ")"; }
}
