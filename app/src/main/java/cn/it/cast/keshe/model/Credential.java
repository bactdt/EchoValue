package cn.it.cast.keshe.model;

public class Credential {
    private long id;
    private String name;
    private String ownerEmail;
    private String username;
    private String passwordEncrypted;
    private String website;
    private String notes;
    private long createdAt;
    private long updatedAt;
    private boolean favorite;

    public Credential() {
    }

    public Credential(long id, String name, String username, String passwordEncrypted,
                      String website, String notes, long createdAt, long updatedAt, boolean favorite) {
        this.id = id;
        this.name = name;
        this.username = username;
        this.passwordEncrypted = passwordEncrypted;
        this.website = website;
        this.notes = notes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.favorite = favorite;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getOwnerEmail() { return ownerEmail; }
    public void setOwnerEmail(String ownerEmail) { this.ownerEmail = ownerEmail; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordEncrypted() { return passwordEncrypted; }
    public void setPasswordEncrypted(String passwordEncrypted) { this.passwordEncrypted = passwordEncrypted; }

    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public boolean isFavorite() { return favorite; }
    public void setFavorite(boolean favorite) { this.favorite = favorite; }
}
