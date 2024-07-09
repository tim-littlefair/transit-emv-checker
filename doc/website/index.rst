===================
Overview
===================

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

Although the primary product of this project is an Android handheld
application using the generic Android NFC API, the codebase also 
contains a command line application written in Java using the PCSC 
Java API which can be used to perform the same analysis and capture
tasks as the Android application using a USB PCSC contactless reader.  
The command line application has been confirmed to run successfully 
on Linux (Ubuntu 24.04 LTS), macOS (Catalina 10.15), and Windows 10, 
but only the Linux integration is presently exercised regularly by
the automated tests run as part of the project's Continuous Integration
infrastructure.

-------------------
Initial motivations
-------------------
The idea for this tool arises from a past employment role working
for a vendor of fare payment systems for transit.  In the course
of introducing cEMV payments for some of our customers we received
a number of support requests in relation to cards issued by 
particular banks and other institutions which were not accepted 
at the transit validators we had provided.

Geographical separation between the transit business customer and 
subject matter experts supporting the system contributed to 
the difficulty of investigating these issues, and restrictions imposed 
on us and the customer by the PCI-DSS 3.X approvals governing these systems
effectively made it impossible for us to make any progress in relation to 
the majority of these requests.

PCI-DSS revision 4.0, was published in 2022, and contains language 
which is less restrictive than the earlier version and may allow 
capture of the sort of data which would enable investigations of this type
and opens the door to the possiblity that a diagnostic capture application
can be used without violating PCI obligations.  

See `PCI_DSS_Considerations`_ for more detailed discussion of this, and
especially the disclaimer subsection of that page.

The TEC app is intended to be used in the hands of customer service
staff at transit businesses accepting cEMV, to capture sufficient 
technical detail in relation to a cEMV media item which is not 
accepted to allow subject matter experts at a different location 
to provide advice to the operator on the cause of rejection.

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
fall under PCI-DSS requirements.  See the subsection `PCI_DSS_HowComply`_ 
on the PCI-DSS Considerations page on this site for more details of
the steps taken to prevent storage of data which would be problematic
for an environment required to comply to PCI.

The user of the Android device is responsible for managing the stored
data responsibly.  It is recommended that stored data will be securely 
shared with cEMV subject matter experts only, and will be deleted from
the Android device within 30 days of storage.


.. _PCI_DSS_Considerations: pci_dss_considerations.html

.. _PCI_DSS_HowComply: pci_dss_considerations.html#how-does-tec-attempt-to-be-compliant-to-pci-dss-v4-0

.. toctree::
    :maxdepth: 2

    overview

    pci_dss_considerations


