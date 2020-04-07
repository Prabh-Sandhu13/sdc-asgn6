// OrderException is a custom exception to handle order placement and updation
public class OrderException extends Exception {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public static String errMessage;
	public static int orderID;
	public OrderException(String errorMessage, int orderId) {
        errMessage = errorMessage;
        orderID = orderId;
    }

	// getMessage() returns error message given while querying 
	public String  getMessage() {
		return errMessage;
	}
	
	// getReference() returns orderid of the order
	public int  getReference() {
		return orderID;
	}
}
