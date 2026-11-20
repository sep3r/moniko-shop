# Pull Request: Implement server-side pagination for admin lists

## Summary
Adds server-side pagination to the admin product and user tables. The admin UI now requests 20 records per page instead of downloading the complete collections.

## Changes
- Add a reusable `AdminPage<T>` response DTO.
- Add `GET /api/admin/products/page` and `GET /api/admin/users/page` endpoints.
- Keep the existing unpaginated endpoints for backward compatibility.
- Validate page and size parameters (page >= 0, size 1..100).
- Sort records by descending ID for stable navigation.
- Update the React admin product and user pages to use the paginated endpoints and the existing Pager component.
- Show total record count and current page in the admin UI.
- Handle empty pages after deleting the last row and return to page one after creating a product.

## API example
`GET /api/admin/products/page?page=0&size=20`

```json
{
  "items": [],
  "page": 0,
  "size": 20,
  "totalItems": 0,
  "totalPages": 0,
  "first": true,
  "last": true
}
```

## Testing
- Frontend build/tests and backend Maven tests could not be run in the provided environment because dependencies/tooling were unavailable (`mvn` is not installed and npm dependency installation did not complete).
- Please run `cd backend && mvn test` and `cd frontend && npm ci && npm run build && npm test` before merging.
