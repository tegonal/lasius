/**
 * Lasius - Open source time tracker for teams
 * Copyright (c) Tegonal Genossenschaft (https://tegonal.com)
 *
 * This file is part of Lasius.
 *
 * Lasius is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * Lasius is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with Lasius.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 */

import { Box } from 'theme-ui';
import React from 'react';

export const Logo: React.FC = () => {
  return (
    <Box sx={{ label: 'Logo' }}>
      <svg
        width="107"
        height="32"
        viewBox="0 0 107 32"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
      >
        <path
          d="M14.4688 16.918C14.6301 16.2702 14.8801 15.6211 15.3484 15.145C15.8889 14.5955 16.6665 14.3357 17.4334 14.2576C19.2848 14.069 21.142 14.8544 22.5532 16.0681C23.964 17.2819 24.9786 18.8916 25.8212 20.5514C26.1851 21.268 25.4816 21.3022 24.8767 21.4058C24.3145 21.502 23.7405 21.5125 23.1715 21.5145C21.6909 21.5195 20.1963 21.4611 18.7618 21.0945C17.3272 20.7281 15.9444 20.0315 14.9614 18.924C14.6989 18.6285 14.4605 18.2916 14.3881 17.9032C14.3273 17.5766 14.3887 17.2403 14.4688 16.918ZM0.909212 17.3891C0.370576 19.9586 0.891508 23.3247 2.72298 25.3266C4.53614 27.3084 7.10944 28.8116 9.5667 29.8459C13.0662 31.3189 17.0036 31.7879 20.719 31.2182C24.4914 30.64 28.6567 29.548 32.0873 27.8495C33.1349 27.331 34.2762 26.6406 35.0025 25.7058C35.9223 24.5218 35.3549 23.5418 34.5106 22.4702C31.5937 18.7681 28.1473 15.2461 24.0934 12.7968C22.2281 11.67 19.7965 10.1622 17.6797 9.76439C15.8393 10.8317 14.2881 11.7424 12.3469 12.9483C13.6428 11.8816 14.939 10.8148 16.235 9.74786C17.5687 8.65011 18.9021 7.55224 20.236 6.45453C22.0112 4.99309 24.3538 3.19318 25.2644 2.60391C26.1751 2.01465 27.3536 1.3504 28.6393 1.31826C29.9249 1.28612 36.2648 1.21111 37.6041 2.12179C38.9433 3.03247 39.0317 4.31813 39.5674 5.06809C40.1031 5.81806 42.1629 6.51064 42.8887 5.5502C44.8171 2.2825 39.3365 0.948599 36.9425 0.621855C34.1854 0.245613 30.4089 -0.111261 28.5321 0.0325958C26.6553 0.176453 25.5991 0.600821 24.5802 1.21017C23.5615 1.81951 21.7897 3.07234 20.156 4.4941C19.2935 5.24468 17.56 6.59082 16.6443 7.27315C16.1373 7.65103 15.6336 8.0254 15.0953 8.36551C14.7368 8.59203 13.7726 9.42115 13.357 9.40133C10.3253 9.25726 7.23243 9.90217 4.85861 11.4985C2.92317 12.8003 1.46571 14.7344 0.909212 17.3891Z"
          fill="currentcolor"
        />
        <path
          d="M51.623 26.8809V28.4141H44.5137V26.8809H51.623ZM44.8848 14.1953V28.4141H43V14.1953H44.8848Z"
          fill="currentcolor"
        />
        <path
          d="M59.2402 15.4551L54.5332 28.4141H52.6094L58.0293 14.1953H59.2695L59.2402 15.4551ZM63.1855 28.4141L58.4688 15.4551L58.4395 14.1953H59.6797L65.1191 28.4141H63.1855Z"
          fill="currentcolor"
        />
        <path
          d="M74.6016 24.8203C74.6016 24.4883 74.5495 24.1953 74.4453 23.9414C74.3477 23.681 74.1719 23.4466 73.918 23.2383C73.6706 23.0299 73.3255 22.8314 72.8828 22.6426C72.4466 22.4538 71.8932 22.2617 71.2227 22.0664C70.5195 21.8581 69.8848 21.627 69.3184 21.373C68.752 21.1126 68.2669 20.8164 67.8633 20.4844C67.4596 20.1523 67.1504 19.7715 66.9355 19.3418C66.7207 18.9121 66.6133 18.4206 66.6133 17.8672C66.6133 17.3138 66.7272 16.8027 66.9551 16.334C67.1829 15.8652 67.5085 15.4583 67.9316 15.1133C68.3613 14.7617 68.8724 14.4883 69.4648 14.293C70.0573 14.0977 70.7181 14 71.4473 14C72.515 14 73.4199 14.2051 74.1621 14.6152C74.9108 15.0189 75.4805 15.5495 75.8711 16.207C76.2617 16.8581 76.457 17.5547 76.457 18.2969H74.582C74.582 17.763 74.4681 17.291 74.2402 16.8809C74.0124 16.4642 73.6673 16.1387 73.2051 15.9043C72.7428 15.6634 72.1569 15.543 71.4473 15.543C70.7767 15.543 70.2233 15.6439 69.7871 15.8457C69.3509 16.0475 69.0254 16.321 68.8105 16.666C68.6022 17.0111 68.498 17.4049 68.498 17.8477C68.498 18.1471 68.5599 18.4206 68.6836 18.668C68.8138 18.9089 69.0124 19.1335 69.2793 19.3418C69.5527 19.5501 69.8978 19.7422 70.3145 19.918C70.7376 20.0938 71.2422 20.263 71.8281 20.4258C72.6354 20.6536 73.332 20.9076 73.918 21.1875C74.5039 21.4674 74.9857 21.7832 75.3633 22.1348C75.7474 22.4798 76.0306 22.8737 76.2129 23.3164C76.4017 23.7526 76.4961 24.2474 76.4961 24.8008C76.4961 25.3802 76.3789 25.9043 76.1445 26.373C75.9102 26.8418 75.5749 27.2422 75.1387 27.5742C74.7025 27.9062 74.1784 28.1634 73.5664 28.3457C72.9609 28.5215 72.2839 28.6094 71.5352 28.6094C70.8776 28.6094 70.2298 28.5182 69.5918 28.3359C68.9603 28.1536 68.3841 27.8802 67.8633 27.5156C67.349 27.151 66.9355 26.7018 66.623 26.168C66.3171 25.6276 66.1641 25.0026 66.1641 24.293H68.0391C68.0391 24.7812 68.1335 25.2012 68.3223 25.5527C68.5111 25.8978 68.7682 26.1842 69.0938 26.4121C69.4258 26.64 69.8001 26.8092 70.2168 26.9199C70.64 27.0241 71.0794 27.0762 71.5352 27.0762C72.1927 27.0762 72.7493 26.985 73.2051 26.8027C73.6608 26.6204 74.0059 26.36 74.2402 26.0215C74.4811 25.6829 74.6016 25.2826 74.6016 24.8203Z"
          fill="currentcolor"
        />
        <path d="M80.9199 14.1953V28.4141H79.0352V14.1953H80.9199Z" fill="currentcolor" />
        <path
          d="M92.4824 14.1953H94.3574V23.8145C94.3574 24.8822 94.1198 25.7708 93.6445 26.4805C93.1693 27.1901 92.5378 27.724 91.75 28.082C90.9688 28.4336 90.1191 28.6094 89.2012 28.6094C88.2376 28.6094 87.3652 28.4336 86.584 28.082C85.8092 27.724 85.194 27.1901 84.7383 26.4805C84.2891 25.7708 84.0645 24.8822 84.0645 23.8145V14.1953H85.9297V23.8145C85.9297 24.5566 86.0664 25.1686 86.3398 25.6504C86.6133 26.1322 86.9941 26.4902 87.4824 26.7246C87.9772 26.959 88.5501 27.0762 89.2012 27.0762C89.8587 27.0762 90.4316 26.959 90.9199 26.7246C91.4147 26.4902 91.7988 26.1322 92.0723 25.6504C92.3457 25.1686 92.4824 24.5566 92.4824 23.8145V14.1953Z"
          fill="currentcolor"
        />
        <path
          d="M104.895 24.8203C104.895 24.4883 104.842 24.1953 104.738 23.9414C104.641 23.681 104.465 23.4466 104.211 23.2383C103.964 23.0299 103.618 22.8314 103.176 22.6426C102.74 22.4538 102.186 22.2617 101.516 22.0664C100.812 21.8581 100.178 21.627 99.6113 21.373C99.0449 21.1126 98.5599 20.8164 98.1562 20.4844C97.7526 20.1523 97.4434 19.7715 97.2285 19.3418C97.0137 18.9121 96.9062 18.4206 96.9062 17.8672C96.9062 17.3138 97.0202 16.8027 97.248 16.334C97.4759 15.8652 97.8014 15.4583 98.2246 15.1133C98.6543 14.7617 99.1654 14.4883 99.7578 14.293C100.35 14.0977 101.011 14 101.74 14C102.808 14 103.713 14.2051 104.455 14.6152C105.204 15.0189 105.773 15.5495 106.164 16.207C106.555 16.8581 106.75 17.5547 106.75 18.2969H104.875C104.875 17.763 104.761 17.291 104.533 16.8809C104.305 16.4642 103.96 16.1387 103.498 15.9043C103.036 15.6634 102.45 15.543 101.74 15.543C101.07 15.543 100.516 15.6439 100.08 15.8457C99.6439 16.0475 99.3184 16.321 99.1035 16.666C98.8952 17.0111 98.791 17.4049 98.791 17.8477C98.791 18.1471 98.8529 18.4206 98.9766 18.668C99.1068 18.9089 99.3053 19.1335 99.5723 19.3418C99.8457 19.5501 100.191 19.7422 100.607 19.918C101.031 20.0938 101.535 20.263 102.121 20.4258C102.928 20.6536 103.625 20.9076 104.211 21.1875C104.797 21.4674 105.279 21.7832 105.656 22.1348C106.04 22.4798 106.324 22.8737 106.506 23.3164C106.695 23.7526 106.789 24.2474 106.789 24.8008C106.789 25.3802 106.672 25.9043 106.438 26.373C106.203 26.8418 105.868 27.2422 105.432 27.5742C104.995 27.9062 104.471 28.1634 103.859 28.3457C103.254 28.5215 102.577 28.6094 101.828 28.6094C101.171 28.6094 100.523 28.5182 99.8848 28.3359C99.2533 28.1536 98.6771 27.8802 98.1562 27.5156C97.6419 27.151 97.2285 26.7018 96.916 26.168C96.61 25.6276 96.457 25.0026 96.457 24.293H98.332C98.332 24.7812 98.4264 25.2012 98.6152 25.5527C98.804 25.8978 99.0612 26.1842 99.3867 26.4121C99.7188 26.64 100.093 26.8092 100.51 26.9199C100.933 27.0241 101.372 27.0762 101.828 27.0762C102.486 27.0762 103.042 26.985 103.498 26.8027C103.954 26.6204 104.299 26.36 104.533 26.0215C104.774 25.6829 104.895 25.2826 104.895 24.8203Z"
          fill="currentcolor"
        />
      </svg>
    </Box>
  );
};