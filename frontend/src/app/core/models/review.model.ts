export interface Review {
  id: number;
  productId: number;
  productName: string;
  productImg: string;
  userId: number;
  userName: string;
  rating: number;
  comment: string;
  createdAt: string;
}

export interface ReviewCreate {
  productId: number;
  rating: number;
  comment: string;
}
