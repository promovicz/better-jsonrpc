package better.jsonrpc.test.simple.model;

public class SimplePerson {

    String firstName;
    String lastName;

    SimpleAddress address;

    public SimplePerson() {
    }

    public SimplePerson(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public SimpleAddress getAddress() {
        return address;
    }

    public void setAddress(SimpleAddress address) {
        this.address = address;
    }

    public boolean equals(SimplePerson other) {
        return this.firstName.equals(other.firstName)
            && this.lastName.equals(other.lastName);
    }

}
