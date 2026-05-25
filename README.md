# Create-From-Requisition

This repository is a fork of [StabilisOne/Create-From-Requisition](https://github.com/StabilisOne/Create-From-Requisition), updated to support the latest core platform standards.

This plugin adds a "Create From" button functionality to the Purchase Order window, allowing users to generate Purchase Orders directly from existing Requisitions.

---

## Compatibility
This fork has been adapted and tested for the following environment:
* iDempiere: Version 13
* Java: 17
* ZK Framework: 10.x

---

## Features
* Adds a standard "Create From" button to the Purchase Order header.
* Filters and pulls open Requisition lines into the Purchase Order.
* Streamlines the procurement workflow by reducing manual data entry.

## Documentation
For detailed installation guides and functional documentation, please visit the official iDempiere Wiki:
[Plugin: Create from requisition](https://wiki.idempiere.org/en/Plugin:_Create_from_requisition)

## Installation
1. Download the `.jar` artifact from the latest release of this fork.
2. Install the plugin via the Felix Web Console or include it in your custom iDempiere build.
3. Pack-in the required metadata (2Pack) to enable the button on the user interface.