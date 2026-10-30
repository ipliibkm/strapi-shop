/** One static service card shown on the home and services fallback layouts. */
export interface ServiceCard {
  number: string;
  symbol: string;
}

/** The three placeholder service cards, shared by the home and services pages. */
export const SERVICE_CARDS: ServiceCard[] = [
  { number: '01', symbol: '✦' },
  { number: '02', symbol: '◈' },
  { number: '03', symbol: '✳' },
];
