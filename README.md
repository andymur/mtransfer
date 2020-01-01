Documentation:
	- Add some for tests (F)
	- Add some for interfaces (F)

AccountResourceTest
	- Add corner cases (T)
	- Add persistence mock (T)

MoneyTransferAcceptanceTest:
	- Add queue for POST operations (T?)

AccountServiceTest
    - Remove shared methods from MoneyTransferAcceptanceTest somewhere (N)
	- Add corner cases for transfer (T)

Application
	- Add banner (F)
	- Fix health check warning (T)
	- Fix dependencies duplication (T)
	- Add normal read me (F)

Other things
	- Make transfer result to be enum (+corner cases for transfer) (N)
	- Calculate preconditions for multi threaded tests (F+)
	- Distinguish between integration (at least final acceptance) and unit tests (T)