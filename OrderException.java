
public class OrderException extends Exception {
	
	public static String errMessage;
	public static int orderID;
	public OrderException(String errorMessage, int orderId) {
        errMessage = errorMessage;
        orderID = orderId;
    }

	public String  getMessage() {
		return errMessage;
	}
	
	public int  getReference() {
		return orderID;
	}
}
