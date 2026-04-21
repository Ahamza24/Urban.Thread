package com.urbanthreads.app;



public class models {
    public static class User implements java.io.Serializable {

        private String name;
        private String email;
        private String role;
        private String profileImage;
        private String location;
        private String businessDescription;
        private String phoneNumber;
        private String shopName;
        private String gender;
        private java.util.List<String> portfolio;
        private java.util.List<String> following; // List of tailor UIDs

        private String id;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        // Empty constructor (REQUIRED for Firebase)
        public User() {}

        public User(String name, String email, String role) {
            this.name = name;
            this.email = email;
            this.role = role;
            this.portfolio = new java.util.ArrayList<>();
            this.following = new java.util.ArrayList<>();
        }

        // Getters and Setters
        public String getName() { return name; }
        public String getEmail() { return email; }
        public String getRole() { return role; }
        public String getProfileImage() { return profileImage; }
        public String getLocation() { return location; }
        public String getBusinessDescription() { return businessDescription; }
        public String getPhoneNumber() { return phoneNumber; }
        public String getShopName() { return shopName; }
        public String getGender() { return gender; }
        public java.util.List<String> getPortfolio() { return portfolio; }
        public java.util.List<String> getFollowing() { return following; }

        public void setName(String name) { this.name = name; }
        public void setEmail(String email) { this.email = email; }
        public void setRole(String role) { this.role = role; }
        public void setProfileImage(String profileImage) { this.profileImage = profileImage; }
        public void setLocation(String location) { this.location = location; }
        public void setBusinessDescription(String businessDescription) { this.businessDescription = businessDescription; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
        public void setShopName(String shopName) { this.shopName = shopName; }
        public void setGender(String gender) { this.gender = gender; }
        public void setPortfolio(java.util.List<String> portfolio) { this.portfolio = portfolio; }
        public void setFollowing(java.util.List<String> following) { this.following = following; }
    }
}
