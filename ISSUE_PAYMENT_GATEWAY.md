# Issue: Add payment gateway integration

## Description
The application currently allows users to place orders without completing an online payment. Integrating a payment gateway would provide a complete checkout flow.

## Proposed features
- Integrate an online payment gateway.
- Add a pending-payment order status.
- Verify payment transactions on the backend using the gateway's server-to-server verification API.
- Automatically cancel unpaid orders after a configurable timeout.
- Restore product stock when an unpaid order is cancelled.

## Expected result
Customers can complete purchases through a secure online payment flow, and order/payment states remain consistent even when a payment attempt fails or times out.

## Priority
Medium
