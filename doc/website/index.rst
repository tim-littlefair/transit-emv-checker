


===================
Transit EMV Checker
===================

----------
Background
----------
Transit EMV Checker (TEC) is an Android mobile device application 
which can be used to access a contactless EMV payment card or 
other cEMV media (phone, wearable...) to perform a technical 
check to determine whether the configuration of the media is likely
to be suitable for acceptance for payment in a transit system where 
acceptance of the media must be determined offline as it is not 
acceptable to permit online verification of payments due to 
timing/usability considerations.

The tool accesses data from the cEMV media including the Primary 
Account Number (PAN), the storage of which is regarded as sensitive 
under the PCI regulations governing payment systems.  The tool
is designed to be acceptable under PCI-DSS revision 4.0 by 
ensuring that at least 6 digits of the PAN are discarded immediately
after they are received from the media and are not stored on 
disk or displayed on the user interface.  As well as the PAN, the 
tool emulates a transit payment terminal and retrieves configuration 
data ("EMV tags") from the cEMV media.  The configuration data 
received is examined and is used to determine whether the media
is able to authenticate itself to the standards required for offline
acceptance, and whether there are any restrictions on usage 
which would prevent it from being accepted for transit payments
at the current location.

Note that the wording of PCI-DSS revisions in the 3.X series 
in relation to processing of live EMV media on test systems was 
more restrictive than the wording in revision 4.0.  The tool is 
not represented as being acceptable under PCI-DSS revisions in 
the 3.X series.

-------------------
Initial motivations
-------------------
The idea for this tool arises from a past employment role working
for a vendor of fare payment systems for transit.  In the course
of introducing cEMV payments for some of our customers we received
a number of support requests in relation to cards issued by 
particular banks and other institutions which were not accepted 
at the transit validators we had provided.

PCI-DSS revision 3.X restrictions made it impossible for logging at 
live validators to be sufficiently detailed to enable investigation 
of these problems.

Although, as noted above, PCI-DSS revision 4.0 is less restrictive,
geographical separation between the transit business customer and 
subject matter experts supporting the system was also a problem 
at this time.

The TEC app is intended to be used in the hands of customer service
staff at transit businesses accepting cEMV, to capture sufficient 
technical detail in relation to a cEMV media item which is not 
accepted to allow subject matter experts at a different location 
to advise on the cause of rejection.

--------------
Current Status
--------------
The application is presently being developed in a private repository 
on GitHub, but will be made public and released under the Apache 2.0 
license at some time in the near future.

The application will also be offered through the Google Play Store.




---------------
Code Repository
---------------
.. list-table:: 
   :header-rows: 1
   :width: 80%
   :stub-columns: 0

   * - Component/Version
     - Hosting URL
   * - GitHub
     - `<TBD>`_

--------------
Privacy Policy
--------------
The TEC application does not directly send any data over networks.
It does store PCI-sanitized data read from each cEMV media presented
in an XML file which can be accessed via the USB storage capability 
of the host Android device.  The PCI-sanitization process masks the 
PAN in the standard way, ensuring that the data stored does not 
fall under PCI-DSS requirements.

The user of the Android device is responsible for managing the stored
data responsibly.  It is recommended that stored data will be securely 
shared with cEMV subject matter experts only, and will be deleted from
the Android device within 30 days of storage.


-----
Other
-----
In the role I had when I developed this itch I would have been the SME (based in Australia while our customers are in UK, Western Europe and the US), but at that time we had no means of capturing a log at the customer site without (a) setting up a validator under conditions which would violate PCI-DSS 3.X requirements and (b) getting the customer's card to a location where a locally employed technician could present the card to the PCI-violating validator.

The language related to live cards and test devices in PCI-DSS v4.0 is a little less restrictive than that in PCI-DSS v3.X, and this, together with the fact that I am no longer directly employed by a payments solution vendor,


.. toctree::
   :maxdepth: 0
   :hidden:

