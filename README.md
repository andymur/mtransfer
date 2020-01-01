Documentation:
	- Add some for tests
	- Add some for interfaces

AccountResourceTest
	- Add corner cases
	- Add persistence mock

MoneyTransferAcceptanceTest:
	- Remove jersey methods to separate place and reuse from AccountResourceTest

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
	- make transfer result to be enum (+corner cases for transfer)