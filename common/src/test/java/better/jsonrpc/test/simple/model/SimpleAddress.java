package better.jsonrpc.test.simple.model;

public class SimpleAddress {

    String city;
    String street;
    String number;

    public SimpleAddress() {
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public boolean equals(SimpleAddress other) {
        return ((this.city == null && other.city == null) || this.city.equals(other.city))
            && ((this.street == null && other.street == null) ||this.street.equals(other.street))
            && ((this.number == null && other.number == null) ||this.number.equals(other.number));
    }

}
