package models;

public class User {
    private String id;
    private String name;
    private String email;
    private String phone;
    private String className;
    private String rollNumber;
    private String passwordHash;
    private int roleId;

    public User(String id, String name, String email, String phone, String className, String rollNumber, String passwordHash, int roleId) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.className = className;
        this.rollNumber = rollNumber;
        this.passwordHash = passwordHash;
        this.roleId = roleId;
    }

    public User(String id, String name, String email, String phone, String passwordHash, int roleId) {
        this(id, name, email, phone, null, null, passwordHash, roleId);
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    public String getRollNumber() { return rollNumber; }
    public void setRollNumber(String rollNumber) { this.rollNumber = rollNumber; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public int getRoleId() { return roleId; }
    public void setRoleId(int roleId) { this.roleId = roleId; }
}
