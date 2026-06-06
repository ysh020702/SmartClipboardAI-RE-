# SmartClipboardAI One UI Palette

This app uses a fixed Samsung One UI inspired palette instead of Android dynamic color.

## Core colors

| Token | Hex | Usage |
| --- | --- | --- |
| Samsung Blue | `#0381FE` | Primary actions, selected tabs, selected chips |
| Samsung Blue Dark | `#0072DE` | Light theme text on blue-tinted surfaces |
| Samsung Blue Dark Mode | `#3E91FF` | Primary action color in dark theme |
| Blue Soft | `#EAF4FF` | Selected items and light focus states |
| Background | `#FAFAFA` | Main light app background |
| Surface | `#FFFFFF` | Cards, app bars, navigation surfaces |
| Raised Surface | `#F5F6F8` | Quiet secondary blocks and thumbnails |
| Text | `#111214` | Primary text |
| Secondary Text | `#6F737A` | Metadata, counts, supporting labels |
| Outline | `#E3E5EA` | Dividers and low emphasis boundaries |

## Usage rules

- Large areas stay monotone: background, surface, and raised surface.
- Samsung Blue is reserved for selected states and primary actions.
- Focus blocks use subdued colors; gradients and loud containers are avoided.
- Rounded blocks follow One UI style: 20-26dp for main content blocks.
- Dynamic color is disabled so the product identity stays consistent.
