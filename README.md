Documentation:
	- Add some for tests
	- Add some for interfaces

AccountResourceTest
	- Add corner cases
	- Add persistence mock

MoneyTransferAcceptanceTest:
	- Remove jersey methods to separate place and reuse from AccountResourceTest
	- Add latch patch
	- Add queue for POST operations

AccountServiceTest
	- Add me

AccountService:
	- Not return default account when it doesn't exist
	- Add corner cases for transfer

Application
	- Add banner
	- Fix health check warning
	- Fix dependencies duplication
	- Add normal read me

Other things
	- Make transfer result to be enum (+corner cases for transfer)
	- Calculate preconditions for multi threaded tests
	- Distinguish between integration (at least final acceptance) and unit tests