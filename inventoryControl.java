// inventoryControl is an interface to implement database update for order shipment,
// reorders, received orders
public interface inventoryControl {
	public void Ship_order( int orderNumber ) throws OrderException;
	public int Issue_reorders( int year, int month, int day );
	public void Receive_order( int internal_order_reference ) throws OrderException;
}
