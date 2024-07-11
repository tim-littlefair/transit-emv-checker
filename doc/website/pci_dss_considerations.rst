======================
PCI-DSS Considerations
======================

As noted in the companion `Overview` page on this site, PCI-DSS 
revision 3.X obligations present a major challenge in relation 
to investigation of EMV cards/mobile wallets/wearables which 
are expected to be accepted in a given transit payment system 
but turn out to be declined or to fail with an error.

The obligation which causes this problem is that there is a blanket 
ban on live cards being presented at test validators or vice versa.  
Between these two restrictions, it was effectively impossible for 
us to investigate issues related to cards or other media from some 
institutions being rejected at validators despite the issuing institution 
expecting that they would be accepted.  

These restrictions also made it challenging to deliver features related 
to mobile wallet applications (Apple Pay, Google Wallet, comparable
products associated with Samsung, other mobile device manufacturers
and EMV-emulating wallet applications associated with financial 
institutions) as there was typically no documented way of establishing
a working mobile wallet using test environment keys.

In PCI-DSS revision 4.0, published in March 2022, the blanket 
ban on presentation of live cards at test validators is relaxed
to a restriction that live cards may only be presented at a test 
environment if the test environment is secured to the same standard
as would be required for a live environment.  

-----------------------------------------------------
How does TEC attempt to be compliant to PCI-DSS v4.0?
-----------------------------------------------------

The present version of the TEC application as built from the publicly 
available source code on Github is designed to truncate all occurrences
of the PAN according to PCI-DSS v4.0 requirement 3.4 and also to overwrite 
the values of all EMV tags classified as 'sensitive authentication data' 
immediately on capture, ensuring that these items are not logged or written 
to storage. 
EMV tags 5F20 'Cardholder Name' and 5F30 'Service Code' are also overwritten.
While TEC does enable the storage of the truncated PAN, and the expiration 
date, it is believed that these are permitted so long as the full PAN is 
not stored with them.  Stakeholders considering adopting TEC for use are
encouraged to inspect source file PCIMaskingAgent.java and run a debugger
over its methods to verify that these items are supressed as promised.

Please notify the developer if inspection or debugging reveals any concerns
that the mechanisms described above are not working as intended.

All future code published on Github, and all versions of the application 
made available via the Android Play Store will filter EMV tags visible
in logging and reporting in this way (although obviously, there is no way of 
preventing developers who check the Github repository out from building 
private builds of the application in which the masking behaviour is not done).


----------
Disclaimer
----------

The developer of TEC has been involved in various PCI certification 
campaigns, but does not claim to be an authority on what is 
and is not acceptable behaviour for any organization or person 
required to comply to any given version of PCI-DSS. 

The expectation of the developer of the TEC application is that:

* It MAY be permissible for TEC to be used by organisations 
  required to comply to PCI-DSS v4.0; BUT
* It is ALMOST CERTAINLY NOT permissible for TEC to be used by
  organisations required to comply to PCI-DSS v3.X; AND
* The existence of PCI-DSS v4.0 IS NOT LIKELY to automatically enable
  organizations who have achieved approval to operate under
  PCI-DSS v3.X to benefit from the more relaxed attitude of v4.0 
  until recertification under v4.0 has been undertaken.

For organizations which own, operate or support an EMV payment solution
which creates PCI-DSS obligations, it is recommended that they seek
advice from their PCI-DSS Qualified Security Assessor (`QSA`_) before
using the TEC application or anything derived from its codebase.

.. _QSA: https://www.pcisecuritystandards.org/assessors_and_solutions/become_qsa/
